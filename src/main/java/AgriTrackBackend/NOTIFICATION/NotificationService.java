package AgriTrackBackend.NOTIFICATION;

import com.google.firebase.messaging.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository repository;

    @Autowired
    private UserNotificationTokenRepository tokenRepository;

    // SAVE
    public NotificationEntity save(
            NotificationEntity entity
    ) {

        entity.setIsSent(false);

        return repository.save(entity);
    }

    // GET ALL
    public List<NotificationEntity> getAll() {

        return repository
                .findByIsActiveTrueOrderByNotificationIdDesc();
    }

    // SEND SINGLE PUSH
    public String sendNotification(
            String token,
            NotificationEntity request
    ) throws Exception {

        Notification notification =
                Notification.builder()
                        .setTitle(request.getTitle())
                        .setBody(request.getSubtitle())
                        .build();

        Message message = Message.builder()

                .setToken(token)

                .setNotification(notification)

                .putData(
                        "imageUrl",
                        request.getImageUrl() == null
                                ? ""
                                : request.getImageUrl()
                )

                .putData(
                        "screenName",
                        request.getScreenName() == null
                                ? ""
                                : request.getScreenName()
                )

                .putData(
                        "timerSeconds",
                        String.valueOf(
                                request.getTimerSeconds() == null
                                        ? 0
                                        : request.getTimerSeconds()
                        )
                )

                .putData(
                        "notificationType",
                        request.getNotificationType() == null
                                ? ""
                                : request.getNotificationType()
                )

                .putData(
                        "clickAction",
                        request.getClickAction() == null
                                ? ""
                                : request.getClickAction()
                )

                .build();

        return FirebaseMessaging
                .getInstance()
                .send(message);
    }

    // SEND TO USER
    public void sendToUser(
            NotificationEntity request
    ) throws Exception {

        List<UserNotificationToken> tokens =
                tokenRepository
                        .findByUserIdAndIsActiveTrue(
                                request.getUserId()
                        );

        for (UserNotificationToken token : tokens) {

            try {

                sendNotification(
                        token.getFcmToken(),
                        request
                );

            } catch (Exception e) {

                System.out.println(
                        "FCM Failed : "
                                + token.getFcmToken()
                );
            }
        }

        request.setIsSent(true);

        repository.save(request);
    }

    // AUTO SEND
    @Scheduled(fixedRate = 10000)
    public void autoSendNotifications() {

        List<NotificationEntity> notifications =
                repository
                        .findByIsSentFalseAndSendAtLessThanEqual(
                                LocalDateTime.now()
                        );

        for (NotificationEntity notification : notifications) {

            try {

                sendToUser(notification);

                System.out.println(
                        "Notification Sent : "
                                + notification.getNotificationId()
                );

            } catch (Exception e) {

                System.out.println(
                        "Notification Failed"
                );

                e.printStackTrace();
            }
        }
    }
}