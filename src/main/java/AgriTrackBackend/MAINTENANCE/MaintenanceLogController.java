package AgriTrackBackend.MAINTENANCE;

import AgriTrackBackend.COMMON.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/maintenance")
@CrossOrigin
@Tag(name = "Maintenance Logs", description = "Service/repair history for an owner's tractors")
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

    @Operation(summary = "Paged/search/filter/sort maintenance log list",
            description = "?search matches maintenanceType. ?tractorId/?from/?to filter exactly/by range. Additive.")
    @GetMapping("/search-paged/{ownerId}")
    public PageResponse<MaintenanceLog> searchPaged(
            @PathVariable Long ownerId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long tractorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        return service.searchPaged(ownerId, search, tractorId, from, to, page, size, sortBy, sortDir);
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
