package AgriTrackBackend.ATTENDANCE;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "DRIVER_ATTENDANCE")
public class DriverAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ATTENDANCE_ID")
    private Long attendanceId;

    @Column(name = "DRIVER_ID", nullable = false)
    private Long driverId;

    @Column(name = "OWNER_ID", nullable = false)
    private Long ownerId;

    @Column(name = "ATTENDANCE_DATE", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "CLOCK_IN_TIME")
    private LocalDateTime clockInTime;

    @Column(name = "CLOCK_OUT_TIME")
    private LocalDateTime clockOutTime;

    @Column(name = "BREAK_MINUTES", nullable = false)
    private Integer breakMinutes = 0;

    @Column(name = "OVERTIME_MINUTES", nullable = false)
    private Integer overtimeMinutes = 0;

    // PRESENT / ABSENT / LEAVE / HALF_DAY
    @Column(name = "STATUS", length = 20, nullable = false)
    private String status = AttendanceStatus.PRESENT;

    @Column(name = "LEAVE_REASON", length = 255)
    private String leaveReason;

    @Column(name = "NOTES", length = 500)
    private String notes;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // ✅ worked minutes for the day, net of break time — used by payroll
    // for overtime detection and reporting. Field access is used for JPA
    // (see @Id on the field above), so this derived getter is never persisted.
    public Long getWorkedMinutes() {
        if (clockInTime == null || clockOutTime == null) return 0L;
        long total = java.time.Duration.between(clockInTime, clockOutTime).toMinutes();
        long net = total - (breakMinutes == null ? 0 : breakMinutes);
        return Math.max(0, net);
    }
}
