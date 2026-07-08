package AgriTrackBackend.RATE;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RateRepository extends JpaRepository<Rate, Long> {

    List<Rate> findByOwnerId(Long ownerId);

    List<Rate> findByOwnerIdAndIsActiveTrue(Long ownerId);

    @Query("SELECT r FROM Rate r WHERE r.ownerId = :ownerId " +
            "AND (:search IS NULL OR LOWER(r.serviceType) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:machineType IS NULL OR r.machineType = :machineType) " +
            "AND (:isActive IS NULL OR r.isActive = :isActive)")
    Page<Rate> search(@Param("ownerId") Long ownerId,
                       @Param("search") String search,
                       @Param("machineType") String machineType,
                       @Param("isActive") Boolean isActive,
                       Pageable pageable);
}
