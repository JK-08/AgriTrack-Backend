package AgriTrackBackend.SESSION;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    List<LoginHistory> findByUserIdOrderByLoginTimeDesc(Long userId);

    // used to close out the most recent open session on logout
    List<LoginHistory> findByUserIdAndLogoutTimeIsNullOrderByLoginTimeDesc(Long userId);

    @Query("SELECT h FROM LoginHistory h WHERE h.userId = :userId " +
            "AND (:status IS NULL OR h.status = :status) " +
            "AND (:platform IS NULL OR h.platform = :platform)")
    Page<LoginHistory> search(@Param("userId") Long userId,
                               @Param("status") String status,
                               @Param("platform") String platform,
                               Pageable pageable);
}
