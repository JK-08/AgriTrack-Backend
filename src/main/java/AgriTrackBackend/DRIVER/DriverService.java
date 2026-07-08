package AgriTrackBackend.DRIVER;

import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.COMMON.PageResponse;
import AgriTrackBackend.COMMON.PaginationUtil;
import AgriTrackBackend.EXCEPTION.ForbiddenException;
import AgriTrackBackend.EXCEPTION.ResourceNotFoundException;
import AgriTrackBackend.SECURITY.CurrentUser;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DriverService {

    @Autowired
    private DriverRepository repository;

    @Autowired
    private AuditService auditService;

    @Transactional
    public Driver save(Driver driver) {
        // ✅ a driver profile is always added by the owner it works for
        driver.setOwnerId(CurrentUser.id());
        if (driver.getStatus() == null) {
            driver.setStatus("ACTIVE");
        }
        if (driver.getIsAvailable() == null) {
            driver.setIsAvailable(true);
        }
        Driver saved = repository.saveAndFlush(driver);
        auditService.log(AuditAction.CREATE, "Driver", saved.getDriverId(), null, saved);
        return saved;
    }

    public List<Driver> getAll() {
        return repository.findAll();
    }

    public List<Driver> getByOwner(Long ownerId) {
        CurrentUser.requireSelf(ownerId);
        return repository.findByOwnerId(ownerId);
    }

    public PageResponse<Driver> searchPaged(Long ownerId, String search, String status, Boolean isAvailable,
                                             Integer page, Integer size, String sortBy, String sortDir) {
        CurrentUser.requireSelf(ownerId);
        Pageable pageable = PaginationUtil.build(page, size, sortBy, sortDir, "createdAt");
        String s = (search == null || search.isBlank()) ? null : search;
        String st = (status == null || status.isBlank()) ? null : status;
        return PageResponse.of(repository.search(ownerId, s, st, isAvailable, pageable));
    }

    public List<Driver> getAvailable(Long ownerId) {
        CurrentUser.requireSelf(ownerId);
        return repository.findByOwnerIdAndIsAvailableTrue(ownerId);
    }

    public Driver getById(Long id) {
        Driver driver = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));
        assertAccess(driver);
        return driver;
    }

    // their own owner, or the driver themselves
    private void assertAccess(Driver driver) {
        Long me = CurrentUser.id();
        if (!driver.getOwnerId().equals(me) && !driver.getUserId().equals(me)) {
            throw new ForbiddenException("You do not have access to this driver");
        }
    }

    public Driver getByUserId(Long userId) {
        CurrentUser.requireSelf(userId);
        return repository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found for userId: " + userId));
    }

    @Transactional
    public Driver update(Long id, Driver data) {
        Driver existing = getById(id);
        if (!existing.getOwnerId().equals(CurrentUser.id())) {
            throw new ForbiddenException("Only the owner can edit a driver's profile");
        }
        String before = auditService.snapshot(existing);
        existing.setLicenseNumber(data.getLicenseNumber());
        existing.setLicenseExpiry(data.getLicenseExpiry());
        existing.setPhotoUrl(data.getPhotoUrl());
        existing.setStatus(data.getStatus());
        existing.setNotes(data.getNotes());
        if (data.getMonthlySalary() != null) {
            existing.setMonthlySalary(data.getMonthlySalary());
        }
        Driver saved = repository.saveAndFlush(existing);
        auditService.logRaw(AuditAction.UPDATE, "Driver", id, before, saved);
        return saved;
    }

    // the driver toggles their own availability; their owner may too
    @Transactional
    public Driver setAvailability(Long id, boolean available) {
        Driver existing = getById(id); // assertAccess already covers owner-or-self
        String before = auditService.snapshot(existing);
        existing.setIsAvailable(available);
        Driver saved = repository.saveAndFlush(existing);
        auditService.logRaw(AuditAction.STATUS_CHANGE, "Driver", id, before, saved);
        return saved;
    }

    @Transactional
    public void deleteById(Long id) {
        Driver existing = getById(id);
        if (!existing.getOwnerId().equals(CurrentUser.id())) {
            throw new ForbiddenException("Only the owner can remove a driver");
        }
        String before = auditService.snapshot(existing);
        repository.delete(existing);
        auditService.logRaw(AuditAction.DELETE, "Driver", id, before, null);
    }
}
