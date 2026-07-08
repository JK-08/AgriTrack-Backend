package AgriTrackBackend.BOOKING;

import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.COMMON.PageResponse;
import AgriTrackBackend.COMMON.PaginationUtil;
import AgriTrackBackend.DRIVER.Driver;
import AgriTrackBackend.DRIVER.DriverRepository;
import AgriTrackBackend.EXCEPTION.ForbiddenException;
import AgriTrackBackend.EXCEPTION.ResourceNotFoundException;
import AgriTrackBackend.SECURITY.CurrentUser;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class BookingService {

    @Autowired
    private BookingRepository repository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private AuditService auditService;

    @Transactional
    public Booking save(Booking booking) {
        // ✅ a booking is always requested by the currently authenticated farmer
        booking.setClientId(CurrentUser.id());
        if (booking.getStatus() == null) {
            booking.setStatus("PENDING");
        }
        Booking saved = repository.saveAndFlush(booking);
        auditService.log(AuditAction.CREATE, "Booking", saved.getBookingId(), null, saved);
        return saved;
    }

    public List<Booking> getAll() {
        return repository.findAll();
    }

    public List<Booking> getByOwner(Long ownerId) {
        CurrentUser.requireSelf(ownerId);
        return repository.findByOwnerIdOrderByBookingIdDesc(ownerId);
    }

    public List<Booking> getByClient(Long clientId) {
        CurrentUser.requireSelf(clientId);
        return repository.findByClientIdOrderByBookingIdDesc(clientId);
    }

    public PageResponse<Booking> searchPaged(Long ownerId, String search, String status,
                                              LocalDateTime from, LocalDateTime to,
                                              Integer page, Integer size, String sortBy, String sortDir) {
        CurrentUser.requireSelf(ownerId);
        Pageable pageable = PaginationUtil.build(page, size, sortBy, sortDir, "createdAt");
        String s = (search == null || search.isBlank()) ? null : search;
        String st = (status == null || status.isBlank()) ? null : status;
        return PageResponse.of(repository.search(ownerId, s, st, from, to, pageable));
    }

    public List<Booking> getPending(Long ownerId) {
        CurrentUser.requireSelf(ownerId);
        return repository.findByOwnerIdAndStatus(ownerId, "PENDING");
    }

    public List<Booking> getByDriver(Long driverId) {
        assertDriverAccess(driverId);
        return repository.findByDriverIdOrderByBookingIdDesc(driverId);
    }

    // a driver may view only their own job list; their owner may view it too
    private void assertDriverAccess(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + driverId));
        Long me = CurrentUser.id();
        if (!driver.getUserId().equals(me) && !driver.getOwnerId().equals(me)) {
            throw new ForbiddenException("You do not have access to this driver's jobs");
        }
    }

    // ✅ OWNER assigns a tractor + driver to their own accepted booking
    @Transactional
    public Booking assignDriver(Long id, Long driverId, Long tractorId) {
        Booking existing = getById(id);
        assertOwner(existing);

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + driverId));
        if (!driver.getOwnerId().equals(existing.getOwnerId())) {
            throw new ForbiddenException("That driver does not belong to you");
        }

        existing.setDriverId(driverId);
        if (tractorId != null) {
            existing.setTractorId(tractorId);
        }
        return repository.saveAndFlush(existing);
    }

    public Booking getById(Long id) {
        Booking booking = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        assertParticipant(booking);
        return booking;
    }

    private void assertOwner(Booking booking) {
        if (!booking.getOwnerId().equals(CurrentUser.id())) {
            throw new ForbiddenException("You do not have access to this booking");
        }
    }

    // owner, requesting client, or the assigned driver may view a booking
    private void assertParticipant(Booking booking) {
        Long me = CurrentUser.id();
        boolean isOwner = booking.getOwnerId() != null && booking.getOwnerId().equals(me);
        boolean isClient = booking.getClientId() != null && booking.getClientId().equals(me);
        boolean isDriver = booking.getDriverId() != null
                && driverRepository.findById(booking.getDriverId()).map(Driver::getUserId).map(me::equals).orElse(false);
        if (!isOwner && !isClient && !isDriver) {
            throw new ForbiddenException("You do not have access to this booking");
        }
    }

    private static final Set<String> OWNER_ONLY_TRANSITIONS = Set.of("ACCEPTED", "REJECTED", "COMPLETED");

    @Transactional
    public Booking changeStatus(Long id, String status) {
        Booking existing = getById(id);
        Long me = CurrentUser.id();

        if (OWNER_ONLY_TRANSITIONS.contains(status)) {
            if (!existing.getOwnerId().equals(me)) {
                throw new ForbiddenException("Only the owner can perform this action");
            }
        } else if ("CANCELLED".equals(status)) {
            if (!existing.getOwnerId().equals(me) && !existing.getClientId().equals(me)) {
                throw new ForbiddenException("You do not have access to this booking");
            }
        }

        String before = auditService.snapshot(existing);
        existing.setStatus(status);
        Booking saved = repository.saveAndFlush(existing);
        auditService.logRaw(AuditAction.STATUS_CHANGE, "Booking", id, before, saved);
        return saved;
    }

    @Transactional
    public void deleteById(Long id) {
        Booking existing = getById(id); // assertParticipant already covers owner/client/driver
        Long me = CurrentUser.id();
        if (!existing.getOwnerId().equals(me) && !existing.getClientId().equals(me)) {
            throw new ForbiddenException("You do not have access to this booking");
        }
        String before = auditService.snapshot(existing);
        repository.delete(existing);
        auditService.logRaw(AuditAction.DELETE, "Booking", id, before, null);
    }
}
