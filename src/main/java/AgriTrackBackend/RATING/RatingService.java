package AgriTrackBackend.RATING;

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
public class RatingService {

    @Autowired
    private RatingRepository repository;

    @Autowired
    private AuditService auditService;

    @Transactional
    public Rating save(Rating rating) {
        // ✅ a rating is always submitted by the currently authenticated client
        rating.setClientId(CurrentUser.id());
        Rating saved = repository.saveAndFlush(rating);
        auditService.log(AuditAction.CREATE, "Rating", saved.getRatingId(), null, saved);
        return saved;
    }

    public List<Rating> getAll() {
        return repository.findAll();
    }

    // ✅ intentionally open — farmers see an owner's ratings before booking (TractorOwnerProfileScreen)
    public List<Rating> getByOwner(Long ownerId) {
        return repository.findByOwnerIdOrderByRatingIdDesc(ownerId);
    }

    // ✅ also intentionally open, same reason as getByOwner above
    public PageResponse<Rating> searchPaged(Long ownerId, Integer minValue, String search,
                                             Integer page, Integer size, String sortBy, String sortDir) {
        Pageable pageable = PaginationUtil.build(page, size, sortBy, sortDir, "createdAt");
        String s = (search == null || search.isBlank()) ? null : search;
        return PageResponse.of(repository.search(ownerId, minValue, s, pageable));
    }

    public List<Rating> getByClient(Long clientId) {
        CurrentUser.requireSelf(clientId);
        return repository.findByClientIdOrderByRatingIdDesc(clientId);
    }

    // ✅ average rating for an owner
    public double getAverageForOwner(Long ownerId) {
        List<Rating> ratings = repository.findByOwnerIdOrderByRatingIdDesc(ownerId);
        if (ratings.isEmpty()) return 0.0;
        double sum = 0;
        int count = 0;
        for (Rating r : ratings) {
            if (r.getRatingValue() != null) {
                sum += r.getRatingValue();
                count++;
            }
        }
        return count == 0 ? 0.0 : Math.round((sum / count) * 10.0) / 10.0;
    }

    public Rating getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rating not found with id: " + id));
    }

    @Transactional
    public void deleteById(Long id) {
        Rating existing = getById(id);
        // only the author of the rating can retract it
        if (existing.getClientId() == null || !existing.getClientId().equals(CurrentUser.id())) {
            throw new ForbiddenException("Only the author can delete this rating");
        }
        String before = auditService.snapshot(existing);
        repository.delete(existing);
        auditService.logRaw(AuditAction.DELETE, "Rating", id, before, null);
    }
}
