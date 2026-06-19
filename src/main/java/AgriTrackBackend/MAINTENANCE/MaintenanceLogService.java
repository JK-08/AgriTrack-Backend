package AgriTrackBackend.MAINTENANCE;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class MaintenanceLogService {

    @Autowired
    private MaintenanceLogRepository repository;

    @Transactional
    public MaintenanceLog save(MaintenanceLog log) {
        if (log.getMaintenanceDate() == null) {
            log.setMaintenanceDate(LocalDate.now());
        }
        return repository.saveAndFlush(log);
    }

    public List<MaintenanceLog> getAll() {
        return repository.findAll();
    }

    public List<MaintenanceLog> getByTractor(Long tractorId) {
        return repository.findByTractorIdOrderByMaintenanceIdDesc(tractorId);
    }

    public List<MaintenanceLog> getByOwner(Long ownerId) {
        return repository.findByOwnerIdOrderByMaintenanceIdDesc(ownerId);
    }

    public MaintenanceLog getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance log not found with id: " + id));
    }

    @Transactional
    public MaintenanceLog update(Long id, MaintenanceLog data) {
        MaintenanceLog existing = getById(id);
        existing.setMaintenanceType(data.getMaintenanceType());
        existing.setCost(data.getCost());
        existing.setMaintenanceDate(data.getMaintenanceDate());
        existing.setNotes(data.getNotes());
        return repository.saveAndFlush(existing);
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
