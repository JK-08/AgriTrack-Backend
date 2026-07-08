package AgriTrackBackend.NOTIFICATION;

import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.COMMON.PageResponse;
import AgriTrackBackend.COMMON.PaginationUtil;
import AgriTrackBackend.EXCEPTION.ForbiddenException;
import AgriTrackBackend.SECURITY.CurrentUser;
import com.google.firebase.messaging.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
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

    @Autowired
    private AuditService auditService;

    @Autowired
    private NotificationPreferenceService preferenceService;

    // SAVE — only owners push notifications out today (e.g. to their customers/drivers)
    public NotificationEntity save(
            NotificationEntity entity
    ) {
        if (!CurrentUser.isRole("OWNER")) {
            throw new ForbiddenException("Only owners can send notifications");
        }

        entity.setIsSent(false);

        NotificationEntity saved = repository.save(entity);
        auditService.log(AuditAction.CREATE, "Notification", saved.getNotificationId(), null, saved);
        return saved;
    }

    // GET ALL
    public List<NotificationEntity> getAll() {

        return repository
                .findByIsActiveTrueOrderByNotificationIdDesc();
    }

    // GET BY USER — used by client/owner/driver apps to show their own alerts
    public List<NotificationEntity> getByUser(Long userId) {
        CurrentUser.requireSelf(userId);
        return repository
                .findByUserIdAndIsActiveTrueOrderByNotificationIdDesc(userId);
    }

    public PageResponse<NotificationEntity> searchPaged(Long userId, String search, String notificationType, Boolean isSent,
                                                          Integer page, Integer size, String sortBy, String sortDir) {
        CurrentUser.requireSelf(userId);
        Pageable pageable = PaginationUtil.build(page, size, sortBy, sortDir, "createdAt");
        String s = (search == null || search.isBlank()) ? null : search;
        String nt = (notificationType == null || notificationType.isBlank()) ? null : notificationType;
        return PageResponse.of(repository.search(userId, s, nt, isSent, pageable));
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

        // ✅ Module 6 — respect the recipient's push preference for this
        // notification type before touching FCM. In-app visibility (the
        // /notification/user/{userId} list) is unaffected — the record is
        // always saved so users can still see it in-app even if push is off.
        boolean pushAllowed = preferenceService.isPushEnabled(request.getUserId(), request.getNotificationType());

        if (pushAllowed) {
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
        }

        request.setIsSent(true);

        NotificationEntity saved = repository.save(request);
        auditService.log(AuditAction.STATUS_CHANGE, "Notification", saved.getNotificationId(), null,
                java.util.Map.of("isSent", true));
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