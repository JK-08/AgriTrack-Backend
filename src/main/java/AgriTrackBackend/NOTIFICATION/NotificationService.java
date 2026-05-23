package AgriTrackBackend.NOTIFICATION;

import com.google.firebase.messaging.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository repository;

    @Autowired
    private UserNotificationTokenRepository tokenRepository;

    // SAVE TEMPLATE
    public NotificationEntity save(NotificationEntity entity) {
        return repository.save(entity);
    }

    // GET ALL
    public List<NotificationEntity> getAll() {
        return repository
                .findByIsActiveTrueOrderByNotificationIdDesc();
    }

    // SEND TO USER
    public String sendNotification(
            String token,
            SendNotificationRequest request
    ) throws Exception {

        Notification notification =
                Notification.builder()
                        .setTitle(request.getTitle())
                        .setBody(request.getSubtitle())
                        .build();

        Message message = Message.builder()

                .setToken(token)

                .setNotification(notification)

                // CUSTOM DATA
                .putData("imageUrl",
                        request.getImageUrl())

                .putData("screenName",
                        request.getScreenName())

                .putData("timerSeconds",
                        String.valueOf(request.getTimerSeconds()))

                .putData("notificationType",
                        request.getNotificationType())

                .putData("clickAction",
                        request.getClickAction())

                .build();

        return FirebaseMessaging
                .getInstance()
                .send(message);
    }

    // SEND TO USER ID
    public void sendToUser(
            Long userId,
            SendNotificationRequest request
    ) throws Exception {

        List<UserNotificationToken> tokens =
                tokenRepository.findByUserId(userId);

        for (UserNotificationToken token : tokens) {

            sendNotification(
                    token.getFcmToken(),
                    request
            );
        }
    }
}