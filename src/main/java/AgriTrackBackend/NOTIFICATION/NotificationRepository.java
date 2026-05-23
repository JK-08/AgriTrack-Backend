package AgriTrackBackend.NOTIFICATION;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository
        extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity>
    findByIsActiveTrueOrderByNotificationIdDesc();


    List<NotificationEntity>
    findByIsSentFalseAndSendAtLessThanEqual(
            LocalDateTime time
    );
}