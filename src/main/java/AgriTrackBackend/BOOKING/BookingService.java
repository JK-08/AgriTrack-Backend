package AgriTrackBackend.BOOKING;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookingService {

    @Autowired
    private BookingRepository repository;

    @Transactional
    public Booking save(Booking booking) {
        if (booking.getStatus() == null) {
            booking.setStatus("PENDING");
        }
        return repository.saveAndFlush(booking);
    }

    public List<Booking> getAll() {
        return repository.findAll();
    }

    public List<Booking> getByOwner(Long ownerId) {
        return repository.findByOwnerIdOrderByBookingIdDesc(ownerId);
    }

    public List<Booking> getByClient(Long clientId) {
        return repository.findByClientIdOrderByBookingIdDesc(clientId);
    }

    public List<Booking> getPending(Long ownerId) {
        return repository.findByOwnerIdAndStatus(ownerId, "PENDING");
    }

    public Booking getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));
    }

    @Transactional
    public Booking changeStatus(Long id, String status) {
        Booking existing = getById(id);
        existing.setStatus(status);
        return repository.saveAndFlush(existing);
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
