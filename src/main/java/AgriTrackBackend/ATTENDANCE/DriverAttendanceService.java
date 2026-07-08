package AgriTrackBackend.ATTENDANCE;

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DriverAttendanceService {

    @Autowired
    private DriverAttendanceRepository repository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private AuditService auditService;

    private Driver loadDriver(Long driverId) {
        return driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + driverId));
    }

    // the driver themselves, or the owner they work for
    private void assertParticipant(Driver driver) {
        Long me = CurrentUser.id();
        if (!driver.getUserId().equals(me) && !driver.getOwnerId().equals(me)) {
            throw new ForbiddenException("You do not have access to this driver's attendance");
        }
    }

    @Transactional
    public DriverAttendance clockIn(Long driverId) {
        Driver driver = loadDriver(driverId);
        assertParticipant(driver);

        LocalDate today = LocalDate.now();
        DriverAttendance attendance = repository.findByDriverIdAndAttendanceDate(driverId, today)
                .orElseGet(() -> {
                    DriverAttendance a = new DriverAttendance();
                    a.setDriverId(driverId);
                    a.setOwnerId(driver.getOwnerId());
                    a.setAttendanceDate(today);
                    return a;
                });
        if (attendance.getClockInTime() != null) {
            throw new RuntimeException("Already clocked in today");
        }
        attendance.setClockInTime(LocalDateTime.now());
        attendance.setStatus(AttendanceStatus.PRESENT);
        DriverAttendance saved = repository.saveAndFlush(attendance);
        auditService.log(AuditAction.CREATE, "DriverAttendance", saved.getAttendanceId(), null, saved);
        return saved;
    }

    @Transactional
    public DriverAttendance clockOut(Long driverId) {
        Driver driver = loadDriver(driverId);
        assertParticipant(driver);

        LocalDate today = LocalDate.now();
        DriverAttendance attendance = repository.findByDriverIdAndAttendanceDate(driverId, today)
                .orElseThrow(() -> new RuntimeException("Not clocked in today"));
        if (attendance.getClockInTime() == null) {
            throw new RuntimeException("Not clocked in today");
        }
        if (attendance.getClockOutTime() != null) {
            throw new RuntimeException("Already clocked out today");
        }
        String before = auditService.snapshot(attendance);
        attendance.setClockOutTime(LocalDateTime.now());

        // anything worked past a standard 8-hour (480 min) day counts as overtime
        long workedMinutes = java.time.Duration.between(attendance.getClockInTime(), attendance.getClockOutTime()).toMinutes()
                - (attendance.getBreakMinutes() == null ? 0 : attendance.getBreakMinutes());
        attendance.setOvertimeMinutes((int) Math.max(0, workedMinutes - 480));

        DriverAttendance saved = repository.saveAndFlush(attendance);
        auditService.logRaw(AuditAction.UPDATE, "DriverAttendance", attendance.getAttendanceId(), before, saved);
        return saved;
    }

    @Transactional
    public DriverAttendance recordBreak(Long driverId, Integer minutes) {
        Driver driver = loadDriver(driverId);
        assertParticipant(driver);
        LocalDate today = LocalDate.now();
        DriverAttendance attendance = repository.findByDriverIdAndAttendanceDate(driverId, today)
                .orElseThrow(() -> new RuntimeException("Not clocked in today"));
        String before = auditService.snapshot(attendance);
        attendance.setBreakMinutes((attendance.getBreakMinutes() == null ? 0 : attendance.getBreakMinutes()) + minutes);
        DriverAttendance saved = repository.saveAndFlush(attendance);
        auditService.logRaw(AuditAction.UPDATE, "DriverAttendance", attendance.getAttendanceId(), before, saved);
        return saved;
    }

    // ✅ owner (or the driver, requesting for themselves) marks a day as leave
    @Transactional
    public DriverAttendance markLeave(LeaveRequest request) {
        Driver driver = loadDriver(request.getDriverId());
        assertParticipant(driver);

        DriverAttendance attendance = repository.findByDriverIdAndAttendanceDate(request.getDriverId(), request.getDate())
                .orElseGet(() -> {
                    DriverAttendance a = new DriverAttendance();
                    a.setDriverId(request.getDriverId());
                    a.setOwnerId(driver.getOwnerId());
                    a.setAttendanceDate(request.getDate());
                    return a;
                });
        boolean isNew = attendance.getAttendanceId() == null;
        String before = isNew ? null : auditService.snapshot(attendance);
        attendance.setStatus(AttendanceStatus.LEAVE);
        attendance.setLeaveReason(request.getReason());
        DriverAttendance saved = repository.saveAndFlush(attendance);
        auditService.logRaw(isNew ? AuditAction.CREATE : AuditAction.STATUS_CHANGE,
                "DriverAttendance", saved.getAttendanceId(), before, saved);
        return saved;
    }

    public PageResponse<DriverAttendance> searchPaged(Long ownerId, Long driverId, String status,
                                                        LocalDate from, LocalDate to,
                                                        Integer page, Integer size, String sortBy, String sortDir) {
        CurrentUser.requireSelf(ownerId);
        Pageable pageable = PaginationUtil.build(page, size, sortBy, sortDir, "attendanceDate");
        String s = (status == null || status.isBlank()) ? null : status.toUpperCase();
        return PageResponse.of(repository.search(ownerId, driverId, s, from, to, pageable));
    }

    public List<DriverAttendance> getByDriverAndRange(Long driverId, LocalDate from, LocalDate to) {
        Driver driver = loadDriver(driverId);
        assertParticipant(driver);
        return repository.findByDriverIdAndAttendanceDateBetween(driverId, from, to);
    }

    public DriverAttendance getById(Long id) {
        DriverAttendance attendance = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found with id: " + id));
        Driver driver = loadDriver(attendance.getDriverId());
        assertParticipant(driver);
        return attendance;
    }

    @Transactional
    public DriverAttendance update(Long id, DriverAttendance data) {
        DriverAttendance existing = getById(id);
        // corrections are an owner-only action — a driver can't retroactively
        // rewrite their own clock-in/out history
        if (!existing.getOwnerId().equals(CurrentUser.id())) {
            throw new ForbiddenException("Only the owner can correct attendance records");
        }
        String before = auditService.snapshot(existing);
        if (data.getStatus() != null) existing.setStatus(data.getStatus());
        if (data.getClockInTime() != null) existing.setClockInTime(data.getClockInTime());
        if (data.getClockOutTime() != null) existing.setClockOutTime(data.getClockOutTime());
        if (data.getBreakMinutes() != null) existing.setBreakMinutes(data.getBreakMinutes());
        if (data.getOvertimeMinutes() != null) existing.setOvertimeMinutes(data.getOvertimeMinutes());
        if (data.getNotes() != null) existing.setNotes(data.getNotes());
        DriverAttendance saved = repository.saveAndFlush(existing);
        auditService.logRaw(AuditAction.UPDATE, "DriverAttendance", id, before, saved);
        return saved;
    }

    @Transactional
    public void deleteById(Long id) {
        DriverAttendance existing = getById(id);
        if (!existing.getOwnerId().equals(CurrentUser.id())) {
            throw new ForbiddenException("Only the owner can delete an attendance record");
        }
        String before = auditService.snapshot(existing);
        repository.delete(existing);
        auditService.logRaw(AuditAction.DELETE, "DriverAttendance", id, before, null);
    }
}
