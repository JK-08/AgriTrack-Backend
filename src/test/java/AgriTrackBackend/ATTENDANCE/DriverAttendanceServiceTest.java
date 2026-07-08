package AgriTrackBackend.ATTENDANCE;

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverAttendanceServiceTest {

    @Mock private DriverAttendanceRepository repository;
    @Mock private DriverRepository driverRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private DriverAttendanceService service;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(Long userId) {
        AuthenticatedUser principal = new AuthenticatedUser(userId, "user@example.com", "DRIVER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private Driver driver(Long driverId, Long userId, Long ownerId) {
        Driver d = new Driver();
        d.setDriverId(driverId);
        d.setUserId(userId);
        d.setOwnerId(ownerId);
        return d;
    }

    @Test
    void clockInCreatesTodaysRecordAndAuditsCreate() {
        loginAs(5L); // the driver themselves
        Driver d = driver(10L, 5L, 1L);
        when(driverRepository.findById(10L)).thenReturn(Optional.of(d));
        when(repository.findByDriverIdAndAttendanceDate(eq(10L), any(LocalDate.class))).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(DriverAttendance.class))).thenAnswer(inv -> {
            DriverAttendance a = inv.getArgument(0);
            a.setAttendanceId(100L);
            return a;
        });

        DriverAttendance result = service.clockIn(10L);

        assertThat(result.getClockInTime()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        verify(auditService).log(eq(AuditAction.CREATE), eq("DriverAttendance"), eq(100L), eq(null), eq(result));
    }

    @Test
    void clockInRejectsStrangerWhoIsNeitherDriverNorOwner() {
        loginAs(99L);
        Driver d = driver(10L, 5L, 1L);
        when(driverRepository.findById(10L)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> service.clockIn(10L)).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void clockInTwiceInSameDayThrows() {
        loginAs(5L);
        Driver d = driver(10L, 5L, 1L);
        when(driverRepository.findById(10L)).thenReturn(Optional.of(d));
        DriverAttendance existing = new DriverAttendance();
        existing.setClockInTime(LocalDateTime.now().minusHours(2));
        when(repository.findByDriverIdAndAttendanceDate(eq(10L), any(LocalDate.class))).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.clockIn(10L)).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Already clocked in");
    }

    @Test
    void clockOutComputesOvertimeBeyondEightHourDay() {
        loginAs(5L);
        Driver d = driver(10L, 5L, 1L);
        when(driverRepository.findById(10L)).thenReturn(Optional.of(d));

        DriverAttendance existing = new DriverAttendance();
        existing.setAttendanceId(100L);
        existing.setDriverId(10L);
        existing.setOwnerId(1L);
        existing.setClockInTime(LocalDateTime.now().minusHours(10)); // worked 10h
        existing.setBreakMinutes(30);
        when(repository.findByDriverIdAndAttendanceDate(eq(10L), any(LocalDate.class))).thenReturn(Optional.of(existing));
        when(auditService.snapshot(existing)).thenReturn("{}");
        when(repository.saveAndFlush(any(DriverAttendance.class))).thenAnswer(inv -> inv.getArgument(0));

        DriverAttendance result = service.clockOut(10L);

        // 600 minutes worked - 30 break = 570 net; 570 - 480 standard = 90 min overtime
        assertThat(result.getOvertimeMinutes()).isEqualTo(90);
        verify(auditService).logRaw(eq(AuditAction.UPDATE), eq("DriverAttendance"), eq(100L), eq("{}"), eq(result));
    }

    @Test
    void clockOutWithoutClockInThrows() {
        loginAs(5L);
        Driver d = driver(10L, 5L, 1L);
        when(driverRepository.findById(10L)).thenReturn(Optional.of(d));
        when(repository.findByDriverIdAndAttendanceDate(eq(10L), any(LocalDate.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.clockOut(10L)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void markLeaveCreatesRecordWhenNoneExistsForThatDate() {
        loginAs(1L); // owner marking leave on behalf of the driver
        Driver d = driver(10L, 5L, 1L);
        when(driverRepository.findById(10L)).thenReturn(Optional.of(d));
        when(repository.findByDriverIdAndAttendanceDate(eq(10L), any(LocalDate.class))).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(DriverAttendance.class))).thenAnswer(inv -> {
            DriverAttendance a = inv.getArgument(0);
            a.setAttendanceId(200L);
            return a;
        });

        LeaveRequest request = new LeaveRequest();
        request.setDriverId(10L);
        request.setDate(LocalDate.now());
        request.setReason("Sick leave");

        DriverAttendance result = service.markLeave(request);

        assertThat(result.getStatus()).isEqualTo(AttendanceStatus.LEAVE);
        assertThat(result.getLeaveReason()).isEqualTo("Sick leave");
        verify(auditService).logRaw(eq(AuditAction.CREATE), eq("DriverAttendance"), eq(200L), eq(null), eq(result));
    }

    @Test
    void updateRejectsDriverCorrectingTheirOwnRecord() {
        loginAs(5L); // the driver, not the owner
        DriverAttendance existing = new DriverAttendance();
        existing.setAttendanceId(100L);
        existing.setDriverId(10L);
        existing.setOwnerId(1L);
        when(repository.findById(100L)).thenReturn(Optional.of(existing));
        when(driverRepository.findById(10L)).thenReturn(Optional.of(driver(10L, 5L, 1L)));

        assertThatThrownBy(() -> service.update(100L, new DriverAttendance())).isInstanceOf(ForbiddenException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deleteByIdRejectsNonOwner() {
        loginAs(2L);
        DriverAttendance existing = new DriverAttendance();
        existing.setAttendanceId(100L);
        existing.setDriverId(10L);
        existing.setOwnerId(1L);
        when(repository.findById(100L)).thenReturn(Optional.of(existing));
        when(driverRepository.findById(10L)).thenReturn(Optional.of(driver(10L, 5L, 1L)));

        assertThatThrownBy(() -> service.deleteById(100L)).isInstanceOf(ForbiddenException.class);
        verify(repository, never()).delete(any());
    }
}
