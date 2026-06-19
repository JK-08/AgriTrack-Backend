package AgriTrackBackend.TRACTOR;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TractorService {

    @Autowired
    private TractorRepository repository;

    @Transactional
    public Tractor save(Tractor tractor) {
        if (tractor.getStatus() == null) {
            tractor.setStatus("AVAILABLE");
        }
        return repository.saveAndFlush(tractor);
    }

    public List<Tractor> getAll() {
        return repository.findAll();
    }

    public List<Tractor> getByOwner(Long ownerId) {
        return repository.findByOwnerId(ownerId);
    }

    public List<Tractor> getAvailable(Long ownerId) {
        return repository.findByOwnerIdAndStatus(ownerId, "AVAILABLE");
    }

    public Tractor getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tractor not found with id: " + id));
    }

    @Transactional
    public Tractor update(Long id, Tractor data) {
        Tractor existing = getById(id);
        existing.setModel(data.getModel());
        existing.setRegistrationNumber(data.getRegistrationNumber());
        existing.setMachineType(data.getMachineType());
        existing.setCapacity(data.getCapacity());
        existing.setHourlyRate(data.getHourlyRate());
        existing.setStatus(data.getStatus());
        existing.setPhotoUrl(data.getPhotoUrl());
        return repository.saveAndFlush(existing);
    }

    @Transactional
    public Tractor updateStatus(Long id, String status) {
        Tractor existing = getById(id);
        existing.setStatus(status);
        return repository.saveAndFlush(existing);
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
