package AgriTrackBackend.MAINTENANCE;

import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.COMMON.PageResponse;
import AgriTrackBackend.COMMON.PaginationUtil;
import AgriTrackBackend.EXCEPTION.ForbiddenException;
import AgriTrackBackend.EXCEPTION.ResourceNotFoundException;
import AgriTrackBackend.SECURITY.CurrentUser;
import AgriTrackBackend.TRACTOR.Tractor;
import AgriTrackBackend.TRACTOR.TractorRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class MaintenanceLogService {

    @Autowired
    private MaintenanceLogRepository repository;

    @Autowired
    private TractorRepository tractorRepository;

    @Autowired
    private AuditService auditService;

    @Transactional
    public MaintenanceLog save(MaintenanceLog log) {
        // ✅ maintenance logs are always the owner's own record
        log.setOwnerId(CurrentUser.id());
        if (log.getMaintenanceDate() == null) {
            log.setMaintenanceDate(LocalDate.now());
        }
        MaintenanceLog saved = repository.saveAndFlush(log);
        auditService.log(AuditAction.CREATE, "MaintenanceLog", saved.getMaintenanceId(), null, saved);
        return saved;
    }

    public List<MaintenanceLog> getAll() {
        return repository.findAll();
    }

    public List<MaintenanceLog> getByTractor(Long tractorId) {
        Tractor tractor = tractorRepository.findById(tractorId)
                .orElseThrow(() -> new ResourceNotFoundException("Tractor not found with id: " + tractorId));
        CurrentUser.requireSelf(tractor.getOwnerId());
        return repository.findByTractorIdOrderByMaintenanceIdDesc(tractorId);
    }

    public List<MaintenanceLog> getByOwner(Long ownerId) {
        CurrentUser.requireSelf(ownerId);
        return repository.findByOwnerIdOrderByMaintenanceIdDesc(ownerId);
    }

    public PageResponse<MaintenanceLog> searchPaged(Long ownerId, String search, Long tractorId, LocalDate from, LocalDate to,
                                                      Integer page, Integer size, String sortBy, String sortDir) {
        CurrentUser.requireSelf(ownerId);
        Pageable pageable = PaginationUtil.build(page, size, sortBy, sortDir, "createdAt");
        String s = (search == null || search.isBlank()) ? null : search;
        return PageResponse.of(repository.search(ownerId, s, tractorId, from, to, pageable));
    }

    public MaintenanceLog getById(Long id) {
        MaintenanceLog log = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance log not found with id: " + id));
        assertOwner(log);
        return log;
    }

    private void assertOwner(MaintenanceLog log) {
        if (log.getOwnerId() == null || !log.getOwnerId().equals(CurrentUser.id())) {
            throw new ForbiddenException("You do not have access to this maintenance log");
        }
    }

    @Transactional
    public MaintenanceLog update(Long id, MaintenanceLog data) {
        MaintenanceLog existing = getById(id);
        String before = auditService.snapshot(existing);
        existing.setMaintenanceType(data.getMaintenanceType());
        existing.setCost(data.getCost());
        existing.setMaintenanceDate(data.getMaintenanceDate());
        existing.setNotes(data.getNotes());
        MaintenanceLog saved = repository.saveAndFlush(existing);
        auditService.logRaw(AuditAction.UPDATE, "MaintenanceLog", id, before, saved);
        return saved;
    }

    @Transactional
    public void deleteById(Long id) {
        MaintenanceLog existing = getById(id);
        String before = auditService.snapshot(existing);
        repository.delete(existing);
        auditService.logRaw(AuditAction.DELETE, "MaintenanceLog", id, before, null);
    }
}
