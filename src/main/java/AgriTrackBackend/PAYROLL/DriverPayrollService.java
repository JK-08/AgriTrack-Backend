package AgriTrackBackend.PAYROLL;

import AgriTrackBackend.ATTENDANCE.AttendanceStatus;
import AgriTrackBackend.ATTENDANCE.DriverAttendance;
import AgriTrackBackend.ATTENDANCE.DriverAttendanceRepository;
import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.COMMON.PageResponse;
import AgriTrackBackend.COMMON.PaginationUtil;
import AgriTrackBackend.DRIVER.Driver;
import AgriTrackBackend.DRIVER.DriverRepository;
import AgriTrackBackend.EXCEPTION.ForbiddenException;
import AgriTrackBackend.EXCEPTION.ResourceNotFoundException;
import AgriTrackBackend.SECURITY.CurrentUser;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
public class DriverPayrollService {

    @Autowired
    private DriverPayrollRepository repository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private DriverAttendanceRepository attendanceRepository;

    @Autowired
    private AuditService auditService;

    private static final BigDecimal OVERTIME_MULTIPLIER = new BigDecimal("1.5");
    private static final BigDecimal STANDARD_HOURS_PER_DAY = BigDecimal.valueOf(8);
    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    // ✅ Generates (or regenerates) a driver's payroll for a month from their
    // recorded attendance — reuses DriverAttendanceRepository instead of
    // duplicating any day-counting logic elsewhere.
    @Transactional
    public DriverPayroll generate(GeneratePayrollRequest request) {
        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + request.getDriverId()));
        CurrentUser.requireSelf(driver.getOwnerId());

        YearMonth ym = YearMonth.parse(request.getMonth());
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        List<DriverAttendance> records = attendanceRepository.findByDriverIdAndAttendanceDateBetween(
                driver.getDriverId(), from, to);

        int presentDays = 0;
        int absentDays = 0;
        int leaveDays = 0;
        int overtimeMinutes = 0;
        double presentUnits = 0; // PRESENT = 1.0, HALF_DAY = 0.5 — used for salary math only

        for (DriverAttendance a : records) {
            if (AttendanceStatus.PRESENT.equals(a.getStatus())) {
                presentDays++;
                presentUnits += 1.0;
            } else if (AttendanceStatus.HALF_DAY.equals(a.getStatus())) {
                presentDays++;
                presentUnits += 0.5;
            } else if (AttendanceStatus.LEAVE.equals(a.getStatus())) {
                leaveDays++;
            } else if (AttendanceStatus.ABSENT.equals(a.getStatus())) {
                absentDays++;
            }
            overtimeMinutes += a.getOvertimeMinutes() == null ? 0 : a.getOvertimeMinutes();
        }

        BigDecimal baseSalary = driver.getMonthlySalary() == null ? BigDecimal.ZERO : driver.getMonthlySalary();
        int daysInMonth = ym.lengthOfMonth();
        BigDecimal perDaySalary = daysInMonth == 0 ? BigDecimal.ZERO
                : baseSalary.divide(BigDecimal.valueOf(daysInMonth), 4, RoundingMode.HALF_UP);
        BigDecimal earnedSalary = perDaySalary.multiply(BigDecimal.valueOf(presentUnits)).setScale(2, RoundingMode.HALF_UP);

        BigDecimal overtimeHourlyRate = perDaySalary.divide(STANDARD_HOURS_PER_DAY, 4, RoundingMode.HALF_UP)
                .multiply(OVERTIME_MULTIPLIER);
        BigDecimal overtimeAmount = BigDecimal.valueOf(overtimeMinutes)
                .divide(MINUTES_PER_HOUR, 4, RoundingMode.HALF_UP)
                .multiply(overtimeHourlyRate)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal incentives = request.getIncentives() == null ? BigDecimal.ZERO : request.getIncentives();
        BigDecimal penalties = request.getPenalties() == null ? BigDecimal.ZERO : request.getPenalties();
        BigDecimal netSalary = earnedSalary.add(overtimeAmount).add(incentives).subtract(penalties);

        DriverPayroll payroll = repository.findByDriverIdAndPayrollMonth(driver.getDriverId(), request.getMonth())
                .orElseGet(DriverPayroll::new);
        boolean isNew = payroll.getPayrollId() == null;
        String before = isNew ? null : auditService.snapshot(payroll);

        payroll.setDriverId(driver.getDriverId());
        payroll.setOwnerId(driver.getOwnerId());
        payroll.setPayrollMonth(request.getMonth());
        payroll.setBaseSalary(baseSalary);
        payroll.setPresentDays(presentDays);
        payroll.setAbsentDays(absentDays);
        payroll.setLeaveDays(leaveDays);
        payroll.setOvertimeMinutes(overtimeMinutes);
        payroll.setOvertimeAmount(overtimeAmount);
        payroll.setIncentives(incentives);
        payroll.setPenalties(penalties);
        payroll.setNetSalary(netSalary);
        payroll.setNotes(request.getNotes());
        if (payroll.getStatus() == null) payroll.setStatus("PENDING");

        DriverPayroll saved = repository.saveAndFlush(payroll);
        auditService.logRaw(isNew ? AuditAction.CREATE : AuditAction.UPDATE,
                "DriverPayroll", saved.getPayrollId(), before, saved);
        return saved;
    }

    public PageResponse<DriverPayroll> searchPaged(Long ownerId, Long driverId, String status, String month,
                                                     Integer page, Integer size, String sortBy, String sortDir) {
        CurrentUser.requireSelf(ownerId);
        Pageable pageable = PaginationUtil.build(page, size, sortBy, sortDir, "payrollMonth");
        String s = (status == null || status.isBlank()) ? null : status.toUpperCase();
        String m = (month == null || month.isBlank()) ? null : month;
        return PageResponse.of(repository.search(ownerId, driverId, s, m, pageable));
    }

    public DriverPayroll getById(Long id) {
        DriverPayroll payroll = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll record not found with id: " + id));
        assertOwner(payroll);
        return payroll;
    }

    private void assertOwner(DriverPayroll payroll) {
        if (payroll.getOwnerId() == null || !payroll.getOwnerId().equals(CurrentUser.id())) {
            throw new ForbiddenException("You do not have access to this payroll record");
        }
    }

    // ✅ Approval/Payment event — marks a generated payroll as paid
    @Transactional
    public DriverPayroll markPaid(Long id) {
        DriverPayroll existing = getById(id);
        String before = auditService.snapshot(existing);
        existing.setStatus("PAID");
        existing.setPaidDate(LocalDateTime.now());
        DriverPayroll saved = repository.saveAndFlush(existing);
        auditService.logRaw(AuditAction.STATUS_CHANGE, "DriverPayroll", id, before, saved);
        return saved;
    }

    @Transactional
    public void deleteById(Long id) {
        DriverPayroll existing = getById(id);
        String before = auditService.snapshot(existing);
        repository.delete(existing);
        auditService.logRaw(AuditAction.DELETE, "DriverPayroll", id, before, null);
    }
}
