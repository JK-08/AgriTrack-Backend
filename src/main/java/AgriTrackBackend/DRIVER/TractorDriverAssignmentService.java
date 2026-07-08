package AgriTrackBackend.DRIVER;

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

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TractorDriverAssignmentService {

    @Autowired
    private TractorDriverAssignmentRepository repository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private TractorRepository tractorRepository;

    @Autowired
    private AuditService auditService;

    // ✅ Assign a driver to a tractor; deactivates any previous active assignment for that tractor
    @Transactional
    public TractorDriverAssignment assign(Long ownerId, Long tractorId, Long driverId) {
        Long me = CurrentUser.id();

        Tractor tractor = tractorRepository.findById(tractorId)
                .orElseThrow(() -> new ResourceNotFoundException("Tractor not found with id: " + tractorId));
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + driverId));

        // the tractor and the driver must both belong to the caller
        if (!tractor.getOwnerId().equals(me) || !driver.getOwnerId().equals(me)) {
            throw new ForbiddenException("You can only assign your own drivers to your own tractors");
        }

        repository.findByTractorIdAndIsActiveTrue(tractorId).ifPresent(old -> {
            String beforeOld = auditService.snapshot(old);
            old.setIsActive(false);
            old.setUnassignedAt(LocalDateTime.now());
            TractorDriverAssignment savedOld = repository.saveAndFlush(old);
            auditService.logRaw(AuditAction.STATUS_CHANGE, "TractorDriverAssignment", savedOld.getAssignmentId(), beforeOld, savedOld);
        });

        TractorDriverAssignment assignment = new TractorDriverAssignment();
        assignment.setOwnerId(me);
        assignment.setTractorId(tractorId);
        assignment.setDriverId(driverId);
        assignment.setIsActive(true);
        assignment.setAssignedAt(LocalDateTime.now());
        TractorDriverAssignment saved = repository.saveAndFlush(assignment);
        auditService.log(AuditAction.CREATE, "TractorDriverAssignment", saved.getAssignmentId(), null, saved);
        return saved;
    }

    @Transactional
    public void unassign(Long assignmentId) {
        TractorDriverAssignment assignment = getById(assignmentId);
        String before = auditService.snapshot(assignment);
        assignment.setIsActive(false);
        assignment.setUnassignedAt(LocalDateTime.now());
        TractorDriverAssignment saved = repository.saveAndFlush(assignment);
        auditService.logRaw(AuditAction.STATUS_CHANGE, "TractorDriverAssignment", assignmentId, before, saved);
    }

    public List<TractorDriverAssignment> getByOwner(Long ownerId) {
        CurrentUser.requireSelf(ownerId);
        return repository.findByOwnerIdOrderByAssignmentIdDesc(ownerId);
    }

    public PageResponse<TractorDriverAssignment> searchPaged(Long ownerId, Boolean isActive, Long driverId, Long tractorId,
                                                               Integer page, Integer size, String sortBy, String sortDir) {
        CurrentUser.requireSelf(ownerId);
        Pageable pageable = PaginationUtil.build(page, size, sortBy, sortDir, "createdAt");
        return PageResponse.of(repository.search(ownerId, isActive, driverId, tractorId, pageable));
    }

    public List<TractorDriverAssignment> getActiveByDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + driverId));
        Long me = CurrentUser.id();
        if (!driver.getUserId().equals(me) && !driver.getOwnerId().equals(me)) {
            throw new ForbiddenException("You do not have access to this driver's assignments");
        }
        return repository.findByDriverIdAndIsActiveTrue(driverId);
    }

    public TractorDriverAssignment getById(Long id) {
        TractorDriverAssignment assignment = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + id));
        if (!assignment.getOwnerId().equals(CurrentUser.id())) {
            throw new ForbiddenException("You do not have access to this assignment");
        }
        return assignment;
    }
}
