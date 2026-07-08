package AgriTrackBackend.DRIVER;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DriverLocationRepository extends JpaRepository<DriverLocation, Long> {

    Optional<DriverLocation> findFirstByDriverIdOrderByLocationIdDesc(Long driverId);

    Optional<DriverLocation> findFirstByBookingIdOrderByLocationIdDesc(Long bookingId);
}
