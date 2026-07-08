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
public interface DriverRepository extends JpaRepository<Driver, Long> {

    List<Driver> findByOwnerId(Long ownerId);

    List<Driver> findByOwnerIdAndIsAvailableTrue(Long ownerId);

    Optional<Driver> findByUserId(Long userId);

    @Query("SELECT d FROM Driver d WHERE d.ownerId = :ownerId " +
            "AND (:search IS NULL OR LOWER(d.licenseNumber) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR d.status = :status) " +
            "AND (:isAvailable IS NULL OR d.isAvailable = :isAvailable)")
    Page<Driver> search(@Param("ownerId") Long ownerId,
                         @Param("search") String search,
                         @Param("status") String status,
                         @Param("isAvailable") Boolean isAvailable,
                         Pageable pageable);
}
