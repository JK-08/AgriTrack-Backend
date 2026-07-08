package AgriTrackBackend.DRIVER;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TractorDriverAssignmentRepository extends JpaRepository<TractorDriverAssignment, Long> {

    List<TractorDriverAssignment> findByOwnerIdOrderByAssignmentIdDesc(Long ownerId);

    List<TractorDriverAssignment> findByDriverIdAndIsActiveTrue(Long driverId);

    Optional<TractorDriverAssignment> findByTractorIdAndIsActiveTrue(Long tractorId);

    @Query("SELECT a FROM TractorDriverAssignment a WHERE a.ownerId = :ownerId " +
            "AND (:isActive IS NULL OR a.isActive = :isActive) " +
            "AND (:driverId IS NULL OR a.driverId = :driverId) " +
            "AND (:tractorId IS NULL OR a.tractorId = :tractorId)")
    Page<TractorDriverAssignment> search(@Param("ownerId") Long ownerId,
                                          @Param("isActive") Boolean isActive,
                                          @Param("driverId") Long driverId,
                                          @Param("tractorId") Long tractorId,
                                          Pageable pageable);
}
