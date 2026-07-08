package AgriTrackBackend.TRACTOR;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TractorRepository extends JpaRepository<Tractor, Long> {

    List<Tractor> findByOwnerId(Long ownerId);

    List<Tractor> findByOwnerIdAndStatus(Long ownerId, String status);

    @Query("SELECT t FROM Tractor t WHERE t.ownerId = :ownerId " +
            "AND (:search IS NULL OR LOWER(t.model) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(t.registrationNumber) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR t.status = :status) " +
            "AND (:machineType IS NULL OR t.machineType = :machineType)")
    Page<Tractor> search(@Param("ownerId") Long ownerId,
                          @Param("search") String search,
                          @Param("status") String status,
                          @Param("machineType") String machineType,
                          Pageable pageable);
}
