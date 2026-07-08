package AgriTrackBackend.AUDIT;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "AUDIT_LOGS")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USER_ID")
    private Long userId;

    @Column(name = "ACTION", nullable = false, length = 50)
    private String action;

    @Column(name = "ENTITY_TYPE", nullable = false, length = 100)
    private String entityType;

    @Column(name = "ENTITY_ID", length = 50)
    private String entityId;

    @Column(name = "BEFORE_VALUE", columnDefinition = "NVARCHAR(MAX)")
    private String beforeValue;

    @Column(name = "AFTER_VALUE", columnDefinition = "NVARCHAR(MAX)")
    private String afterValue;

    @Column(name = "IP_ADDRESS", length = 64)
    private String ipAddress;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
