package AgriTrackBackend.DRIVER;

import AgriTrackBackend.COMMON.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/driver")
@CrossOrigin
@Tag(name = "Drivers", description = "Driver profiles belonging to an owner's fleet")
public class DriverController {

    @Autowired
    private DriverService service;

    @Operation(summary = "Add a driver profile", description = "ownerId is always taken from the caller's JWT.")
    @PostMapping("/create")
    public Driver create(@RequestBody Driver driver) {
        return service.save(driver);
    }

    @GetMapping("/getAll")
    public List<Driver> getAll() {
        return service.getAll();
    }

    @GetMapping("/getByOwner/{ownerId}")
    public List<Driver> getByOwner(@PathVariable Long ownerId) {
        return service.getByOwner(ownerId);
    }

    @Operation(summary = "Paged/search/filter/sort driver list",
            description = "?search matches licenseNumber. ?status and ?isAvailable filter exactly. Additive.")
    @GetMapping("/search-paged/{ownerId}")
    public PageResponse<Driver> searchPaged(
            @PathVariable Long ownerId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isAvailable,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        return service.searchPaged(ownerId, search, status, isAvailable, page, size, sortBy, sortDir);
    }

    @GetMapping("/available/{ownerId}")
    public List<Driver> available(@PathVariable Long ownerId) {
        return service.getAvailable(ownerId);
    }

    @GetMapping("/getById/{id}")
    public Driver getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/getByUser/{userId}")
    public Driver getByUserId(@PathVariable Long userId) {
        return service.getByUserId(userId);
    }

    @PutMapping("/update/{id}")
    public Driver update(@PathVariable Long id, @RequestBody Driver driver) {
        return service.update(id, driver);
    }

    // ✅ DRIVER toggles their own availability
    @PutMapping("/availability/{id}")
    public Driver setAvailability(@PathVariable Long id, @RequestParam boolean available) {
        return service.setAvailability(id, available);
    }

    @DeleteMapping("/deleteById/{id}")
    public String deleteById(@PathVariable Long id) {
        service.deleteById(id);
        return "Driver deleted successfully";
    }
}
