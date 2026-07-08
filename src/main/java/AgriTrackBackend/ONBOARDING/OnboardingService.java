package AgriTrackBackend.ONBOARDING;

import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.COMMON.PageResponse;
import AgriTrackBackend.COMMON.PaginationUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OnboardingService {

    @Autowired
    private OnboardingRepository repository;

    @Autowired
    private AuditService auditService;

    // ✅ MUST HAVE TRANSACTION
    @Transactional
    public Onboarding save(Onboarding onboarding) {
        boolean isNew = onboarding.getOnboardingId() == null;
        Onboarding saved = repository.saveAndFlush(onboarding); // force DB insert
        auditService.log(isNew ? AuditAction.CREATE : AuditAction.UPDATE, "Onboarding", saved.getOnboardingId(), null, saved);
        return saved;
    }

    public List<Onboarding> getAll() {
        return repository.findAll();
    }

    public PageResponse<Onboarding> searchPaged(String search, Integer page, Integer size, String sortBy, String sortDir) {
        Pageable pageable = PaginationUtil.build(page, size, sortBy, sortDir, "createdAt");
        String s = (search == null || search.isBlank()) ? null : search;
        return PageResponse.of(repository.search(s, pageable));
    }

    public Onboarding getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Onboarding not found with id: " + id));
    }

    @Transactional
    public Onboarding update(Long id, Onboarding updatedData) {
        Onboarding existing = getById(id);
        String before = auditService.snapshot(existing);

        existing.setTitle(updatedData.getTitle());
        existing.setSubtitle(updatedData.getSubtitle());
        existing.setImageUrl(updatedData.getImageUrl());

        Onboarding saved = repository.saveAndFlush(existing);
        auditService.logRaw(AuditAction.UPDATE, "Onboarding", id, before, saved);
        return saved;
    }

    @Transactional
    public void deleteById(Long id) {
        Onboarding existing = getById(id);
        String before = auditService.snapshot(existing);
        repository.deleteById(id);
        auditService.logRaw(AuditAction.DELETE, "Onboarding", id, before, null);
    }

    @Transactional
    public void deleteAll() {
        repository.deleteAll();
        auditService.log(AuditAction.DELETE, "Onboarding", "ALL", null, null);
    }
}
