package AgriTrackBackend.DRIVER;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "DRIVERS")
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DRIVER_ID")
    private Long driverId;

    // the USERS row (ROLE = DRIVER) this profile belongs to
    @Column(name = "USER_ID", nullable = false, unique = true)
    private Long userId;

    // the OWNER (USERS.USER_ID) this driver works for
    @Column(name = "OWNER_ID", nullable = false)
    private Long ownerId;

    @Column(name = "LICENSE_NUMBER", length = 50)
    private String licenseNumber;

    @Column(name = "LICENSE_EXPIRY")
    private LocalDate licenseExpiry;

    @Column(name = "PHOTO_URL", length = 500)
    private String photoUrl;

    // ACTIVE / INACTIVE
    @Column(name = "STATUS", length = 20)
    private String status = "ACTIVE";

    @Column(name = "IS_AVAILABLE")
    private Boolean isAvailable = true;

    @Column(name = "NOTES", length = 500)
    private String notes;

    // base monthly salary used by DRIVER_PAYROLL to compute net salary
    @Column(name = "MONTHLY_SALARY", precision = 12, scale = 2)
    private BigDecimal monthlySalary;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
