package AgriTrackBackend.ONBOARDING;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OnboardingService {

    @Autowired
    private OnboardingRepository repository;

    // ✅ MUST HAVE TRANSACTION
    @Transactional
    public Onboarding save(Onboarding onboarding) {
        return repository.saveAndFlush(onboarding); // force DB insert
    }

    public List<Onboarding> getAll() {
        return repository.findAll();
    }

    public Onboarding getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Onboarding not found with id: " + id));
    }

    @Transactional
    public Onboarding update(Long id, Onboarding updatedData) {
        Onboarding existing = getById(id);

        existing.setTitle(updatedData.getTitle());
        existing.setSubtitle(updatedData.getSubtitle());
        existing.setImageUrl(updatedData.getImageUrl());

        return repository.saveAndFlush(existing);
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public void deleteAll() {
        repository.deleteAll();
    }
}