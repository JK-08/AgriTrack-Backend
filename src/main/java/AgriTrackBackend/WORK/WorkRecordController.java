package AgriTrackBackend.WORK;

import AgriTrackBackend.COMMON.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/work")
@CrossOrigin
@Tag(name = "Work Records", description = "Timer-based or manual work sessions performed for a customer, by an owner or an assigned driver.")
public class WorkRecordController {

    @Autowired
    private WorkRecordService service;

    // ✅ TIMER: START
    @Operation(summary = "Start a timed work session", description = "Owner working the tractor themselves, or a driver starting an assigned job.")
    @PostMapping("/start")
    public WorkRecord start(@RequestBody StartWorkRequest request) {
        return service.start(request);
    }

    // ✅ TIMER: PAUSE
    @Operation(summary = "Pause a running work session")
    @PutMapping("/pause/{id}")
    public WorkRecord pause(@PathVariable Long id) {
        return service.pause(id);
    }

    // ✅ TIMER: RESUME
    @Operation(summary = "Resume a paused work session")
    @PutMapping("/resume/{id}")
    public WorkRecord resume(@PathVariable Long id) {
        return service.resume(id);
    }

    // ✅ TIMER: STOP / COMPLETE
    @Operation(summary = "Stop/complete a work session", description = "Finalizes duration and computed amount; optional extra charges.")
    @PutMapping("/stop/{id}")
    public WorkRecord stop(@PathVariable Long id,
                           @RequestParam(required = false) BigDecimal extraCharges) {
        return service.stop(id, extraCharges);
    }

    // ✅ MANUAL ENTRY
    @Operation(summary = "Log a work record manually", description = "No live timer — owner enters duration directly.")
    @PostMapping("/manual")
    public WorkRecord manual(@RequestBody WorkRecord workRecord) {
        return service.saveManual(workRecord);
    }

    @GetMapping("/getAll")
    public List<WorkRecord> getAll() {
        return service.getAll();
    }

    @GetMapping("/getByOwner/{ownerId}")
    public List<WorkRecord> getByOwner(@PathVariable Long ownerId) {
        return service.getByOwner(ownerId);
    }

    @Operation(summary = "Paged/search/filter/sort work record list", description = "?status/?from/?to filter exactly. Additive.")
    @GetMapping("/search-paged/{ownerId}")
    public PageResponse<WorkRecord> searchPaged(
            @PathVariable Long ownerId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        return service.searchPaged(ownerId, search, status, from, to, page, size, sortBy, sortDir);
    }

    @GetMapping("/getByCustomer/{customerId}")
    public List<WorkRecord> getByCustomer(@PathVariable Long customerId) {
        return service.getByCustomer(customerId);
    }

    @GetMapping("/getByDriver/{driverId}")
    public List<WorkRecord> getByDriver(@PathVariable Long driverId) {
        return service.getByDriver(driverId);
    }

    @GetMapping("/getById/{id}")
    public WorkRecord getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @Operation(summary = "Delete a work record", description = "Only the owner may delete (403 otherwise).")
    @DeleteMapping("/deleteById/{id}")
    public String deleteById(@PathVariable Long id) {
        service.deleteById(id);
        return "Work record deleted successfully";
    }
}
