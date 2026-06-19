package AgriTrackBackend.MAINTENANCE;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "MAINTENANCE_LOGS")
public class MaintenanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MAINTENANCE_ID")
    private Long maintenanceId;

    @Column(name = "TRACTOR_ID", nullable = false)
    private Long tractorId;

    @Column(name = "OWNER_ID")
    private Long ownerId;

    @Column(name = "MAINTENANCE_TYPE", length = 100)
    private String maintenanceType;

    @Column(name = "COST", precision = 12, scale = 2)
    private BigDecimal cost;

    @Column(name = "MAINTENANCE_DATE")
    private LocalDate maintenanceDate;

    @Column(name = "NOTES", length = 500)
    private String notes;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
