package AgriTrackBackend.MAINTENANCE;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceLogRepository extends JpaRepository<MaintenanceLog, Long> {

    List<MaintenanceLog> findByTractorIdOrderByMaintenanceIdDesc(Long tractorId);

    List<MaintenanceLog> findByOwnerIdOrderByMaintenanceIdDesc(Long ownerId);
}
