package AgriTrackBackend.DRIVER;

import AgriTrackBackend.COMMON.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assignment")
@CrossOrigin
@Tag(name = "Tractor-Driver Assignments", description = "Which driver is currently assigned to which tractor")
public class TractorDriverAssignmentController {

    @Autowired
    private TractorDriverAssignmentService service;

    @Operation(summary = "Assign a driver to a tractor", description = "Both must belong to the caller. Deactivates any prior active assignment for that tractor.")
    @PostMapping("/assign")
    public TractorDriverAssignment assign(@RequestBody AssignDriverRequest request) {
        return service.assign(request.getOwnerId(), request.getTractorId(), request.getDriverId());
    }

    @PutMapping("/unassign/{id}")
    public String unassign(@PathVariable Long id) {
        service.unassign(id);
        return "Assignment ended";
    }

    @GetMapping("/getByOwner/{ownerId}")
    public List<TractorDriverAssignment> getByOwner(@PathVariable Long ownerId) {
        return service.getByOwner(ownerId);
    }

    @Operation(summary = "Paged/filter/sort assignment list", description = "?isActive/?driverId/?tractorId filter exactly. Additive.")
    @GetMapping("/search-paged/{ownerId}")
    public PageResponse<TractorDriverAssignment> searchPaged(
            @PathVariable Long ownerId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) Long tractorId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        return service.searchPaged(ownerId, isActive, driverId, tractorId, page, size, sortBy, sortDir);
    }

    @GetMapping("/activeByDriver/{driverId}")
    public List<TractorDriverAssignment> getActiveByDriver(@PathVariable Long driverId) {
        return service.getActiveByDriver(driverId);
    }
}
