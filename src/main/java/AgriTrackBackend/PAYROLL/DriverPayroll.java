package AgriTrackBackend.PAYROLL;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "DRIVER_PAYROLL")
public class DriverPayroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PAYROLL_ID")
    private Long payrollId;

    @Column(name = "DRIVER_ID", nullable = false)
    private Long driverId;

    @Column(name = "OWNER_ID", nullable = false)
    private Long ownerId;

    // "yyyy-MM"
    @Column(name = "PAYROLL_MONTH", length = 7, nullable = false)
    private String payrollMonth;

    @Column(name = "BASE_SALARY", precision = 12, scale = 2, nullable = false)
    private BigDecimal baseSalary = BigDecimal.ZERO;

    @Column(name = "PRESENT_DAYS", nullable = false)
    private Integer presentDays = 0;

    @Column(name = "ABSENT_DAYS", nullable = false)
    private Integer absentDays = 0;

    @Column(name = "LEAVE_DAYS", nullable = false)
    private Integer leaveDays = 0;

    @Column(name = "OVERTIME_MINUTES", nullable = false)
    private Integer overtimeMinutes = 0;

    @Column(name = "OVERTIME_AMOUNT", precision = 12, scale = 2, nullable = false)
    private BigDecimal overtimeAmount = BigDecimal.ZERO;

    @Column(name = "INCENTIVES", precision = 12, scale = 2, nullable = false)
    private BigDecimal incentives = BigDecimal.ZERO;

    @Column(name = "PENALTIES", precision = 12, scale = 2, nullable = false)
    private BigDecimal penalties = BigDecimal.ZERO;

    @Column(name = "NET_SALARY", precision = 12, scale = 2, nullable = false)
    private BigDecimal netSalary = BigDecimal.ZERO;

    // PENDING / PAID
    @Column(name = "STATUS", length = 20, nullable = false)
    private String status = "PENDING";

    @Column(name = "PAID_DATE")
    private LocalDateTime paidDate;

    @Column(name = "NOTES", length = 500)
    private String notes;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
