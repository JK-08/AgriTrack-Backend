package AgriTrackBackend.MAINTENANCE;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MaintenanceLogRepository extends JpaRepository<MaintenanceLog, Long> {

    List<MaintenanceLog> findByTractorIdOrderByMaintenanceIdDesc(Long tractorId);

    List<MaintenanceLog> findByOwnerIdOrderByMaintenanceIdDesc(Long ownerId);

    @Query("SELECT m FROM MaintenanceLog m WHERE m.ownerId = :ownerId " +
            "AND (:search IS NULL OR LOWER(m.maintenanceType) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:tractorId IS NULL OR m.tractorId = :tractorId) " +
            "AND (:from IS NULL OR m.maintenanceDate >= :from) " +
            "AND (:to IS NULL OR m.maintenanceDate <= :to)")
    Page<MaintenanceLog> search(@Param("ownerId") Long ownerId,
                                 @Param("search") String search,
                                 @Param("tractorId") Long tractorId,
                                 @Param("from") LocalDate from,
                                 @Param("to") LocalDate to,
                                 Pageable pageable);
}
