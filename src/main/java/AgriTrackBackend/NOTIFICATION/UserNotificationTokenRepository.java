package AgriTrackBackend.NOTIFICATION;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserNotificationTokenRepository
        extends JpaRepository<UserNotificationToken, Long> {

    List<UserNotificationToken>
    findByUserIdAndIsActiveTrue(Long userId);
    List<UserNotificationToken> findByUserId(Long userId);

    Optional<UserNotificationToken>
    findByFcmToken(String fcmToken);
}