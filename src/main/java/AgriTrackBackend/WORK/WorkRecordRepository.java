package AgriTrackBackend.WORK;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkRecordRepository extends JpaRepository<WorkRecord, Long> {

    List<WorkRecord> findByOwnerIdOrderByWorkIdDesc(Long ownerId);

    List<WorkRecord> findByCustomerIdOrderByWorkIdDesc(Long customerId);

    List<WorkRecord> findByOwnerIdAndStatus(Long ownerId, String status);

    List<WorkRecord> findByDriverIdOrderByWorkIdDesc(Long driverId);

    @Query("SELECT w FROM WorkRecord w WHERE w.ownerId = :ownerId " +
            "AND (:search IS NULL OR LOWER(w.serviceType) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR w.status = :status) " +
            "AND (:from IS NULL OR w.workDate >= :from) " +
            "AND (:to IS NULL OR w.workDate <= :to)")
    Page<WorkRecord> search(@Param("ownerId") Long ownerId,
                             @Param("search") String search,
                             @Param("status") String status,
                             @Param("from") LocalDate from,
                             @Param("to") LocalDate to,
                             Pageable pageable);
}
