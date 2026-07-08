package AgriTrackBackend.BOOKING;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByOwnerIdOrderByBookingIdDesc(Long ownerId);

    List<Booking> findByClientIdOrderByBookingIdDesc(Long clientId);

    List<Booking> findByOwnerIdAndStatus(Long ownerId, String status);

    List<Booking> findByDriverIdOrderByBookingIdDesc(Long driverId);

    @Query("SELECT b FROM Booking b WHERE b.ownerId = :ownerId " +
            "AND (:search IS NULL OR LOWER(b.serviceType) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(b.location) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR b.status = :status) " +
            "AND (:from IS NULL OR b.requestedDate >= :from) " +
            "AND (:to IS NULL OR b.requestedDate <= :to)")
    Page<Booking> search(@Param("ownerId") Long ownerId,
                          @Param("search") String search,
                          @Param("status") String status,
                          @Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to,
                          Pageable pageable);
}
