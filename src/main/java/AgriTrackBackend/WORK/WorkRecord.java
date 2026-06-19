package AgriTrackBackend.WORK;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "WORK_RECORDS")
public class WorkRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "WORK_ID")
    private Long workId;

    @Column(name = "OWNER_ID", nullable = false)
    private Long ownerId;

    @Column(name = "CUSTOMER_ID")
    private Long customerId;

    @Column(name = "TRACTOR_ID")
    private Long tractorId;

    @Column(name = "RATE_ID")
    private Long rateId;

    @Column(name = "SERVICE_TYPE", length = 100)
    private String serviceType;

    @Column(name = "WORK_DATE")
    private LocalDate workDate;

    @Column(name = "START_TIME")
    private LocalDateTime startTime;

    @Column(name = "END_TIME")
    private LocalDateTime endTime;

    // marks when the timer was last (re)started, used to accumulate elapsed time
    @Column(name = "LAST_RESUME_TIME")
    private LocalDateTime lastResumeTime;

    // total counted seconds excluding paused gaps
    @Column(name = "ACCUMULATED_SECONDS")
    private Long accumulatedSeconds = 0L;

    @Column(name = "DURATION_MINUTES")
    private Long durationMinutes;

    @Column(name = "AMOUNT", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "EXTRA_CHARGES", precision = 12, scale = 2)
    private BigDecimal extraCharges;

    // RUNNING / PAUSED / COMPLETED / CANCELLED
    @Column(name = "STATUS", length = 20)
    private String status;

    @Column(name = "NOTES", length = 500)
    private String notes;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
