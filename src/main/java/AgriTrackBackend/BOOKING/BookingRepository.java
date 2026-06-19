package AgriTrackBackend.BOOKING;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByOwnerIdOrderByBookingIdDesc(Long ownerId);

    List<Booking> findByClientIdOrderByBookingIdDesc(Long clientId);

    List<Booking> findByOwnerIdAndStatus(Long ownerId, String status);
}
