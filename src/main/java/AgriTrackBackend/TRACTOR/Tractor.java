package AgriTrackBackend.TRACTOR;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "TRACTORS")
public class Tractor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRACTOR_ID")
    private Long tractorId;

    @Column(name = "OWNER_ID", nullable = false)
    private Long ownerId;

    @Column(name = "MODEL", length = 150)
    private String model;

    @Column(name = "REGISTRATION_NUMBER", length = 50)
    private String registrationNumber;

    // TRACTOR / HARVESTER / ROTAVATOR ...
    @Column(name = "MACHINE_TYPE", length = 50)
    private String machineType;

    @Column(name = "CAPACITY", length = 50)
    private String capacity;

    @Column(name = "HOURLY_RATE", precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    // AVAILABLE / BUSY / MAINTENANCE
    @Column(name = "STATUS", length = 20)
    private String status;

    @Column(name = "PHOTO_URL", length = 500)
    private String photoUrl;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
