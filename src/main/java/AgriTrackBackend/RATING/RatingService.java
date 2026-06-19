package AgriTrackBackend.RATING;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RatingService {

    @Autowired
    private RatingRepository repository;

    @Transactional
    public Rating save(Rating rating) {
        return repository.saveAndFlush(rating);
    }

    public List<Rating> getAll() {
        return repository.findAll();
    }

    public List<Rating> getByOwner(Long ownerId) {
        return repository.findByOwnerIdOrderByRatingIdDesc(ownerId);
    }

    public List<Rating> getByClient(Long clientId) {
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
                .orElseThrow(() -> new RuntimeException("Rating not found with id: " + id));
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
