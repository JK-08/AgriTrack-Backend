package AgriTrackBackend.NOTIFICATION;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "NOTIFICATION_PREFERENCES")
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PREFERENCE_ID")
    private Long preferenceId;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "NOTIFICATION_TYPE", length = 30, nullable = false)
    private String notificationType;

    @Column(name = "PUSH_ENABLED", nullable = false)
    private Boolean pushEnabled = true;

    @Column(name = "IN_APP_ENABLED", nullable = false)
    private Boolean inAppEnabled = true;

    @Column(name = "SMS_ENABLED", nullable = false)
    private Boolean smsEnabled = false;

    @Column(name = "EMAIL_ENABLED", nullable = false)
    private Boolean emailEnabled = false;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
}
