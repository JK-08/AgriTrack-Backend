package AgriTrackBackend.NOTIFICATION;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "USER_NOTIFICATION_TOKENS")
public class UserNotificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TOKEN_ID")
    private Long tokenId;

    @Column(name = "USER_ID")
    private Long userId;

    @Column(name = "FCM_TOKEN", length = 500, unique = true)
    private String fcmToken;

    @Column(name = "DEVICE_TYPE")
    private String deviceType;

    @Column(name = "IS_ACTIVE")
    private Boolean isActive = true;

    @Column(
            name = "CREATED_AT",
            insertable = false,
            updatable = false
    )
    private LocalDateTime createdAt;
}