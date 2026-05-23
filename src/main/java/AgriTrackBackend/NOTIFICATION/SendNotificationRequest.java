package AgriTrackBackend.NOTIFICATION;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendNotificationRequest {

    private Long userId;

    private String title;

    private String subtitle;

    private String imageUrl;

    private String screenName;

    private Integer timerSeconds;

    private String notificationType;

    private String clickAction;

    // FORMAT:
    // 2026-05-23T20:30:00
    private String sendAt;
}