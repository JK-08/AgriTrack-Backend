package AgriTrackBackend.DRIVER;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "TRACTOR_DRIVER_ASSIGNMENTS")
public class TractorDriverAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ASSIGNMENT_ID")
    private Long assignmentId;

    @Column(name = "OWNER_ID", nullable = false)
    private Long ownerId;

    @Column(name = "TRACTOR_ID", nullable = false)
    private Long tractorId;

    @Column(name = "DRIVER_ID", nullable = false)
    private Long driverId;

    @Column(name = "IS_ACTIVE")
    private Boolean isActive = true;

    @Column(name = "ASSIGNED_AT")
    private LocalDateTime assignedAt;

    @Column(name = "UNASSIGNED_AT")
    private LocalDateTime unassignedAt;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
