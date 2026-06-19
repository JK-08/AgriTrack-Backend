package AgriTrackBackend.RATE;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RateService {

    @Autowired
    private RateRepository repository;

    @Transactional
    public Rate save(Rate rate) {
        if (rate.getIsActive() == null) {
            rate.setIsActive(true);
        }
        return repository.saveAndFlush(rate);
    }

    public List<Rate> getAll() {
        return repository.findAll();
    }

    public List<Rate> getByOwner(Long ownerId) {
        return repository.findByOwnerId(ownerId);
    }

    public List<Rate> getActiveByOwner(Long ownerId) {
        return repository.findByOwnerIdAndIsActiveTrue(ownerId);
    }

    public Rate getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rate not found with id: " + id));
    }

    @Transactional
    public Rate update(Long id, Rate data) {
        Rate existing = getById(id);
        existing.setServiceType(data.getServiceType());
        existing.setMachineType(data.getMachineType());
        existing.setPricePerMinute(data.getPricePerMinute());
        existing.setPricePerTenMinutes(data.getPricePerTenMinutes());
        existing.setPricePerHour(data.getPricePerHour());
        if (data.getIsActive() != null) {
            existing.setIsActive(data.getIsActive());
        }
        return repository.saveAndFlush(existing);
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
