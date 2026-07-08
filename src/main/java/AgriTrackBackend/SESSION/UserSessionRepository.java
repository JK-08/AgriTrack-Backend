package AgriTrackBackend.SESSION;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);

    List<UserSession> findByUserIdAndRevokedFalseOrderByLastActivityDesc(Long userId);

    List<UserSession> findByUserIdOrderByLoginTimeDesc(Long userId);

    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId " +
            "AND (:revoked IS NULL OR s.revoked = :revoked) " +
            "AND (:platform IS NULL OR s.platform = :platform)")
    Page<UserSession> search(@Param("userId") Long userId,
                              @Param("revoked") Boolean revoked,
                              @Param("platform") String platform,
                              Pageable pageable);
}
