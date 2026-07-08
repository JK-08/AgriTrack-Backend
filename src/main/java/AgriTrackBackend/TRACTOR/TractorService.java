package AgriTrackBackend.TRACTOR;

import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.COMMON.PageResponse;
import AgriTrackBackend.COMMON.PaginationUtil;
import AgriTrackBackend.EXCEPTION.ForbiddenException;
import AgriTrackBackend.EXCEPTION.ResourceNotFoundException;
import AgriTrackBackend.SECURITY.CurrentUser;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TractorService {

    @Autowired
    private TractorRepository repository;

    @Autowired
    private AuditService auditService;

    @Transactional
    public Tractor save(Tractor tractor) {
        tractor.setOwnerId(CurrentUser.id());
        if (tractor.getStatus() == null) {
            tractor.setStatus("AVAILABLE");
        }
        Tractor saved = repository.saveAndFlush(tractor);
        auditService.log(AuditAction.CREATE, "Tractor", saved.getTractorId(), null, saved);
        return saved;
    }

    public List<Tractor> getAll() {
        return repository.findAll();
    }

    public List<Tractor> getByOwner(Long ownerId) {
        CurrentUser.requireSelf(ownerId);
        return repository.findByOwnerId(ownerId);
    }

    public List<Tractor> getAvailable(Long ownerId) {
        CurrentUser.requireSelf(ownerId);
        return repository.findByOwnerIdAndStatus(ownerId, "AVAILABLE");
    }

    public PageResponse<Tractor> searchPaged(Long ownerId, String search, String status, String machineType,
                                              Integer page, Integer size, String sortBy, String sortDir) {
        CurrentUser.requireSelf(ownerId);
        Pageable pageable = PaginationUtil.build(page, size, sortBy, sortDir, "createdAt");
        Page<Tractor> result = repository.search(ownerId, blank(search), blank(status), blank(machineType), pageable);
        return PageResponse.of(result);
    }

    private String blank(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    public Tractor getById(Long id) {
        Tractor tractor = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tractor not found with id: " + id));
        // Farmers browsing available tractors also hit this — only block cross-owner writes,
        // reads stay open since TractorSearchScreen (any customer) needs to view any tractor.
        return tractor;
    }

    private void assertOwner(Tractor tractor) {
        if (tractor.getOwnerId() == null || !tractor.getOwnerId().equals(CurrentUser.id())) {
            throw new ForbiddenException("You do not have access to this tractor");
        }
    }

    @Transactional
    public Tractor update(Long id, Tractor data) {
        Tractor existing = getById(id);
        assertOwner(existing);
        String before = auditService.snapshot(existing);
        existing.setModel(data.getModel());
        existing.setRegistrationNumber(data.getRegistrationNumber());
        existing.setMachineType(data.getMachineType());
        existing.setCapacity(data.getCapacity());
        existing.setHourlyRate(data.getHourlyRate());
        existing.setStatus(data.getStatus());
        existing.setPhotoUrl(data.getPhotoUrl());
        Tractor saved = repository.saveAndFlush(existing);
        auditService.logRaw(AuditAction.UPDATE, "Tractor", id, before, saved);
        return saved;
    }

    @Transactional
    public Tractor updateStatus(Long id, String status) {
        Tractor existing = getById(id);
        assertOwner(existing);
        String before = auditService.snapshot(existing);
        existing.setStatus(status);
        Tractor saved = repository.saveAndFlush(existing);
        auditService.logRaw(AuditAction.STATUS_CHANGE, "Tractor", id, before, saved);
        return saved;
    }

    @Transactional
    public void deleteById(Long id) {
        Tractor existing = getById(id);
        assertOwner(existing);
        String before = auditService.snapshot(existing);
        repository.delete(existing);
        auditService.logRaw(AuditAction.DELETE, "Tractor", id, before, null);
    }
}
