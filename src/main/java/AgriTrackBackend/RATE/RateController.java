package AgriTrackBackend.RATE;

import AgriTrackBackend.COMMON.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rate")
@CrossOrigin
@Tag(name = "Rates", description = "An owner's service pricing. /active/{ownerId} is intentionally open to any authenticated user (farmers browsing before booking).")
public class RateController {

    @Autowired
    private RateService service;

    @PostMapping("/create")
    public Rate create(@RequestBody Rate rate) {
        return service.save(rate);
    }

    @GetMapping("/getAll")
    public List<Rate> getAll() {
        return service.getAll();
    }

    @GetMapping("/getByOwner/{ownerId}")
    public List<Rate> getByOwner(@PathVariable Long ownerId) {
        return service.getByOwner(ownerId);
    }

    @Operation(summary = "Paged/search/filter/sort rate list",
            description = "?search matches serviceType. ?machineType/?isActive filter exactly. Additive.")
    @GetMapping("/search-paged/{ownerId}")
    public PageResponse<Rate> searchPaged(
            @PathVariable Long ownerId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String machineType,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        return service.searchPaged(ownerId, search, machineType, isActive, page, size, sortBy, sortDir);
    }

    @GetMapping("/active/{ownerId}")
    public List<Rate> active(@PathVariable Long ownerId) {
        return service.getActiveByOwner(ownerId);
    }

    @GetMapping("/getById/{id}")
    public Rate getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/update/{id}")
    public Rate update(@PathVariable Long id, @RequestBody Rate rate) {
        return service.update(id, rate);
    }

    @DeleteMapping("/deleteById/{id}")
    public String deleteById(@PathVariable Long id) {
        service.deleteById(id);
        return "Rate deleted successfully";
    }
}
