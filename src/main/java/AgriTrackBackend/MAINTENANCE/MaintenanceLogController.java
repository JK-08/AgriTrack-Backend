package AgriTrackBackend.MAINTENANCE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/maintenance")
@CrossOrigin
public class MaintenanceLogController {

    @Autowired
    private MaintenanceLogService service;

    @PostMapping("/create")
    public MaintenanceLog create(@RequestBody MaintenanceLog log) {
        return service.save(log);
    }

    @GetMapping("/getAll")
    public List<MaintenanceLog> getAll() {
        return service.getAll();
    }

    @GetMapping("/getByTractor/{tractorId}")
    public List<MaintenanceLog> getByTractor(@PathVariable Long tractorId) {
        return service.getByTractor(tractorId);
    }

    @GetMapping("/getByOwner/{ownerId}")
    public List<MaintenanceLog> getByOwner(@PathVariable Long ownerId) {
        return service.getByOwner(ownerId);
    }

    @GetMapping("/getById/{id}")
    public MaintenanceLog getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/update/{id}")
    public MaintenanceLog update(@PathVariable Long id, @RequestBody MaintenanceLog log) {
        return service.update(id, log);
    }

    @DeleteMapping("/deleteById/{id}")
    public String deleteById(@PathVariable Long id) {
        service.deleteById(id);
        return "Maintenance log deleted successfully";
    }
}
