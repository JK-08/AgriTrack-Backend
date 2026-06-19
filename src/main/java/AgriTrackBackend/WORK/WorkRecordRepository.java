package AgriTrackBackend.WORK;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkRecordRepository extends JpaRepository<WorkRecord, Long> {

    List<WorkRecord> findByOwnerIdOrderByWorkIdDesc(Long ownerId);

    List<WorkRecord> findByCustomerIdOrderByWorkIdDesc(Long customerId);

    List<WorkRecord> findByOwnerIdAndStatus(Long ownerId, String status);
}
