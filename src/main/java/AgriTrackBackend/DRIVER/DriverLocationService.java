package AgriTrackBackend.DRIVER;

import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.BOOKING.Booking;
import AgriTrackBackend.BOOKING.BookingRepository;
import AgriTrackBackend.EXCEPTION.ForbiddenException;
import AgriTrackBackend.EXCEPTION.ResourceNotFoundException;
import AgriTrackBackend.SECURITY.CurrentUser;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DriverLocationService {

    @Autowired
    private DriverLocationRepository repository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private AuditService auditService;

    @Transactional
    public DriverLocation record(DriverLocation location) {
        // ✅ only the driver themselves can post their own GPS position
        Driver driver = driverRepository.findById(location.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
        if (!driver.getUserId().equals(CurrentUser.id())) {
            throw new ForbiddenException("You are not this driver");
        }
        DriverLocation saved = repository.saveAndFlush(location);
        // high-frequency event — record it, but don't include lat/long noise in the "before" (there is none, it's a pure create)
        java.util.Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("driverId", saved.getDriverId());
        summary.put("bookingId", saved.getBookingId());
        auditService.log(AuditAction.CREATE, "DriverLocation", saved.getLocationId(), null, summary);
        return saved;
    }

    public DriverLocation latestByDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
        Long me = CurrentUser.id();
        if (!driver.getUserId().equals(me) && !driver.getOwnerId().equals(me)) {
            throw new ForbiddenException("You do not have access to this driver's location");
        }
        return repository.findFirstByDriverIdOrderByLocationIdDesc(driverId).orElse(null);
    }

    public DriverLocation latestByBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        Long me = CurrentUser.id();
        boolean isOwner = booking.getOwnerId() != null && booking.getOwnerId().equals(me);
        boolean isClient = booking.getClientId() != null && booking.getClientId().equals(me);
        boolean isDriver = booking.getDriverId() != null
                && driverRepository.findById(booking.getDriverId()).map(Driver::getUserId).map(me::equals).orElse(false);
        if (!isOwner && !isClient && !isDriver) {
            throw new ForbiddenException("You do not have access to this booking's location");
        }
        return repository.findFirstByBookingIdOrderByLocationIdDesc(bookingId).orElse(null);
    }
}
