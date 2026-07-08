package AgriTrackBackend.AUDIT;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:userId IS NULL OR a.userId = :userId) " +
            "AND (:entityType IS NULL OR a.entityType = :entityType) " +
            "AND (:entityId IS NULL OR a.entityId = :entityId) " +
            "AND (:action IS NULL OR a.action = :action)")
    Page<AuditLog> search(@Param("userId") Long userId,
                           @Param("entityType") String entityType,
                           @Param("entityId") String entityId,
                           @Param("action") String action,
                           Pageable pageable);
}
