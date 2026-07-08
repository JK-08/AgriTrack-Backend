package AgriTrackBackend.PAYROLL;

import AgriTrackBackend.ATTENDANCE.AttendanceStatus;
import AgriTrackBackend.ATTENDANCE.DriverAttendance;
import AgriTrackBackend.ATTENDANCE.DriverAttendanceRepository;
import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.DRIVER.Driver;
import AgriTrackBackend.DRIVER.DriverRepository;
import AgriTrackBackend.EXCEPTION.ForbiddenException;
import AgriTrackBackend.SECURITY.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverPayrollServiceTest {

    @Mock private DriverPayrollRepository repository;
    @Mock private DriverRepository driverRepository;
    @Mock private DriverAttendanceRepository attendanceRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private DriverPayrollService service;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(Long userId) {
        AuthenticatedUser principal = new AuthenticatedUser(userId, "owner@example.com", "OWNER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private DriverAttendance att(LocalDate date, String status, int overtimeMinutes) {
        DriverAttendance a = new DriverAttendance();
        a.setAttendanceDate(date);
        a.setStatus(status);
        a.setOvertimeMinutes(overtimeMinutes);
        return a;
    }

    @Test
    void generateRejectsRequestForAnotherOwnersDriver() {
        loginAs(1L);
        Driver driver = new Driver();
        driver.setDriverId(10L);
        driver.setOwnerId(2L); // belongs to a different owner
        when(driverRepository.findById(10L)).thenReturn(Optional.of(driver));

        GeneratePayrollRequest request = new GeneratePayrollRequest();
        request.setDriverId(10L);
        request.setMonth(YearMonth.now().toString());

        assertThatThrownBy(() -> service.generate(request)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void generateComputesNetSalaryFromAttendanceAndIncentivesMinusPenalties() {
        loginAs(1L);
        YearMonth month = YearMonth.of(2026, 1); // 31-day month, deterministic for the assertion
        Driver driver = new Driver();
        driver.setDriverId(10L);
        driver.setOwnerId(1L);
        driver.setMonthlySalary(new BigDecimal("31000")); // -> exactly 1000/day for a clean assertion
        when(driverRepository.findById(10L)).thenReturn(Optional.of(driver));

        List<DriverAttendance> records = List.of(
                att(month.atDay(1), AttendanceStatus.PRESENT, 0),
                att(month.atDay(2), AttendanceStatus.PRESENT, 60), // 1 hour overtime
                att(month.atDay(3), AttendanceStatus.HALF_DAY, 0),
                att(month.atDay(4), AttendanceStatus.ABSENT, 0),
                att(month.atDay(5), AttendanceStatus.LEAVE, 0)
        );
        when(attendanceRepository.findByDriverIdAndAttendanceDateBetween(10L, month.atDay(1), month.atEndOfMonth()))
                .thenReturn(records);
        when(repository.findByDriverIdAndPayrollMonth(10L, month.toString())).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(DriverPayroll.class))).thenAnswer(inv -> {
            DriverPayroll p = inv.getArgument(0);
            p.setPayrollId(500L);
            return p;
        });

        GeneratePayrollRequest request = new GeneratePayrollRequest();
        request.setDriverId(10L);
        request.setMonth(month.toString());
        request.setIncentives(new BigDecimal("200"));
        request.setPenalties(new BigDecimal("50"));

        DriverPayroll result = service.generate(request);

        // presentUnits = 1 + 1 + 0.5 = 2.5 -> earned = 2500.00
        // overtime: perDay 1000 / 8h = 125/h * 1.5 = 187.5/h; 1h -> 187.50
        // net = 2500 + 187.50 + 200 - 50 = 2837.50
        assertThat(result.getPresentDays()).isEqualTo(3); // 2 PRESENT + 1 HALF_DAY
        assertThat(result.getAbsentDays()).isEqualTo(1);
        assertThat(result.getLeaveDays()).isEqualTo(1);
        assertThat(result.getOvertimeMinutes()).isEqualTo(60);
        assertThat(result.getNetSalary()).isEqualByComparingTo("2837.50");
        verify(auditService).logRaw(eq(AuditAction.CREATE), eq("DriverPayroll"), eq(500L), eq(null), eq(result));
    }

    @Test
    void generateIsIdempotentAndUpdatesExistingRecordForSameDriverAndMonth() {
        loginAs(1L);
        YearMonth month = YearMonth.now();
        Driver driver = new Driver();
        driver.setDriverId(10L);
        driver.setOwnerId(1L);
        driver.setMonthlySalary(new BigDecimal("30000"));
        when(driverRepository.findById(10L)).thenReturn(Optional.of(driver));
        when(attendanceRepository.findByDriverIdAndAttendanceDateBetween(eq(10L), any(), any())).thenReturn(List.of());

        DriverPayroll existing = new DriverPayroll();
        existing.setPayrollId(500L);
        existing.setStatus("PENDING");
        when(repository.findByDriverIdAndPayrollMonth(10L, month.toString())).thenReturn(Optional.of(existing));
        when(auditService.snapshot(existing)).thenReturn("{}");
        when(repository.saveAndFlush(any(DriverPayroll.class))).thenAnswer(inv -> inv.getArgument(0));

        GeneratePayrollRequest request = new GeneratePayrollRequest();
        request.setDriverId(10L);
        request.setMonth(month.toString());

        service.generate(request);

        verify(auditService).logRaw(eq(AuditAction.UPDATE), eq("DriverPayroll"), eq(500L), eq("{}"), any());
    }

    @Test
    void markPaidAuditsAsStatusChangeAndStampsPaidDate() {
        loginAs(1L);
        DriverPayroll existing = new DriverPayroll();
        existing.setPayrollId(500L);
        existing.setOwnerId(1L);
        existing.setStatus("PENDING");
        when(repository.findById(500L)).thenReturn(Optional.of(existing));
        when(auditService.snapshot(existing)).thenReturn("{\"status\":\"PENDING\"}");
        when(repository.saveAndFlush(any(DriverPayroll.class))).thenAnswer(inv -> inv.getArgument(0));

        DriverPayroll result = service.markPaid(500L);

        assertThat(result.getStatus()).isEqualTo("PAID");
        assertThat(result.getPaidDate()).isNotNull();
        verify(auditService).logRaw(eq(AuditAction.STATUS_CHANGE), eq("DriverPayroll"), eq(500L),
                eq("{\"status\":\"PENDING\"}"), eq(result));
    }

    @Test
    void getByIdRejectsNonOwner() {
        loginAs(2L);
        DriverPayroll existing = new DriverPayroll();
        existing.setPayrollId(500L);
        existing.setOwnerId(1L);
        when(repository.findById(500L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.getById(500L)).isInstanceOf(ForbiddenException.class);
    }
}
