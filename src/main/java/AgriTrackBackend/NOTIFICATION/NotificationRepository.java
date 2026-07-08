package AgriTrackBackend.NOTIFICATION;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    List<NotificationEntity>
    findByUserIdAndIsActiveTrueOrderByNotificationIdDesc(Long userId);

    @Query("SELECT n FROM NotificationEntity n WHERE n.userId = :userId " +
            "AND (:search IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:notificationType IS NULL OR n.notificationType = :notificationType) " +
            "AND (:isSent IS NULL OR n.isSent = :isSent)")
    Page<NotificationEntity> search(@Param("userId") Long userId,
                                     @Param("search") String search,
                                     @Param("notificationType") String notificationType,
                                     @Param("isSent") Boolean isSent,
                                     Pageable pageable);
}