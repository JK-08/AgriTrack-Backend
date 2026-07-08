package AgriTrackBackend.TRACTOR;

import AgriTrackBackend.COMMON.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tractor")
@CrossOrigin
@Tag(name = "Tractors", description = "An owner's tractor fleet.")
public class TractorController {

    @Autowired
    private TractorService service;

    @Operation(summary = "Add a tractor", description = "ownerId is always taken from the caller's JWT.")
    @PostMapping("/create")
    public Tractor create(@RequestBody Tractor tractor) {
        return service.save(tractor);
    }

    @GetMapping("/getAll")
    public List<Tractor> getAll() {
        return service.getAll();
    }

    @GetMapping("/getByOwner/{ownerId}")
    public List<Tractor> getByOwner(@PathVariable Long ownerId) {
        return service.getByOwner(ownerId);
    }

    @GetMapping("/available/{ownerId}")
    public List<Tractor> available(@PathVariable Long ownerId) {
        return service.getAvailable(ownerId);
    }

    @Operation(summary = "Paged/search/filter/sort tractor list", description = "?status/?machineType filter exactly. Additive.")
    @GetMapping("/search-paged/{ownerId}")
    public PageResponse<Tractor> searchPaged(
            @PathVariable Long ownerId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String machineType,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        return service.searchPaged(ownerId, search, status, machineType, page, size, sortBy, sortDir);
    }

    @GetMapping("/getById/{id}")
    public Tractor getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @Operation(summary = "Update a tractor", description = "Only the owning owner may update (403 otherwise).")
    @PutMapping("/update/{id}")
    public Tractor update(@PathVariable Long id, @RequestBody Tractor tractor) {
        return service.update(id, tractor);
    }

    @Operation(summary = "Update tractor status", description = "e.g. AVAILABLE/IN_USE/MAINTENANCE.")
    @PutMapping("/status/{id}")
    public Tractor updateStatus(@PathVariable Long id, @RequestParam String status) {
        return service.updateStatus(id, status);
    }

    @Operation(summary = "Delete a tractor", description = "Only the owning owner may delete (403 otherwise).")
    @DeleteMapping("/deleteById/{id}")
    public String deleteById(@PathVariable Long id) {
        service.deleteById(id);
        return "Tractor deleted successfully";
    }
}
