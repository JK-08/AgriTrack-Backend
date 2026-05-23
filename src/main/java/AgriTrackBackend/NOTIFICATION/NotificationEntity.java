package AgriTrackBackend.NOTIFICATION;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "NOTIFICATIONS")
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NOTIFICATION_ID")
    private Long notificationId;

    @Column(name = "USER_ID")
    private Long userId;

    @Column(name = "TITLE")
    private String title;

    @Column(name = "SUBTITLE", length = 500)
    private String subtitle;

    @Column(name = "IMAGE_URL", length = 1000)
    private String imageUrl;

    @Column(name = "SCREEN_NAME")
    private String screenName;

    @Column(name = "TIMER_SECONDS")
    private Integer timerSeconds;

    @Column(name = "NOTIFICATION_TYPE")
    private String notificationType;

    @Column(name = "CLICK_ACTION")
    private String clickAction;

    @Column(name = "IS_ACTIVE")
    private Boolean isActive = true;

    @Column(name = "IS_SENT")
    private Boolean isSent = false;

    @Column(name = "SEND_AT")
    private LocalDateTime sendAt;

    @Column(name = "CREATED_AT",
            insertable = false,
            updatable = false)
    private LocalDateTime createdAt;
}