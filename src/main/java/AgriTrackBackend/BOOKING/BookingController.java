package AgriTrackBackend.BOOKING;

import AgriTrackBackend.COMMON.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/booking")
@CrossOrigin
@Tag(name = "Bookings", description = "Client service requests to an owner: create, assign driver/tractor, accept/reject/complete/cancel.")
public class BookingController {

    @Autowired
    private BookingService service;

    // ✅ CLIENT requests a service
    @Operation(summary = "Create a booking request", description = "Client requesting a service from an owner.")
    @PostMapping("/create")
    public Booking create(@RequestBody Booking booking) {
        return service.save(booking);
    }

    @GetMapping("/getAll")
    public List<Booking> getAll() {
        return service.getAll();
    }

    @GetMapping("/getByOwner/{ownerId}")
    public List<Booking> getByOwner(@PathVariable Long ownerId) {
        return service.getByOwner(ownerId);
    }

    @GetMapping("/getByClient/{clientId}")
    public List<Booking> getByClient(@PathVariable Long clientId) {
        return service.getByClient(clientId);
    }

    @Operation(summary = "Paged/search/filter/sort booking list", description = "?status/?from/?to filter exactly. Additive.")
    @GetMapping("/search-paged/{ownerId}")
    public PageResponse<Booking> searchPaged(
            @PathVariable Long ownerId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        return service.searchPaged(ownerId, search, status, from, to, page, size, sortBy, sortDir);
    }

    @GetMapping("/pending/{ownerId}")
    public List<Booking> pending(@PathVariable Long ownerId) {
        return service.getPending(ownerId);
    }

    @GetMapping("/getById/{id}")
    public Booking getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/getByDriver/{driverId}")
    public List<Booking> getByDriver(@PathVariable Long driverId) {
        return service.getByDriver(driverId);
    }

    // ✅ OWNER assigns a driver (and optionally the tractor) to a booking
    @Operation(summary = "Assign driver/tractor to a booking", description = "Assignment Change event — audited.")
    @PutMapping("/assignDriver/{id}")
    public Booking assignDriver(@PathVariable Long id,
                                 @RequestParam Long driverId,
                                 @RequestParam(required = false) Long tractorId) {
        return service.assignDriver(id, driverId, tractorId);
    }

    // ✅ OWNER accepts
    @Operation(summary = "Accept a booking", description = "Approval event — audited as a status change.")
    @PutMapping("/accept/{id}")
    public Booking accept(@PathVariable Long id) {
        return service.changeStatus(id, "ACCEPTED");
    }

    // ✅ OWNER rejects
    @Operation(summary = "Reject a booking", description = "Rejection event — audited as a status change.")
    @PutMapping("/reject/{id}")
    public Booking reject(@PathVariable Long id) {
        return service.changeStatus(id, "REJECTED");
    }

    // ✅ OWNER marks complete
    @Operation(summary = "Mark a booking complete")
    @PutMapping("/complete/{id}")
    public Booking complete(@PathVariable Long id) {
        return service.changeStatus(id, "COMPLETED");
    }

    // ✅ CLIENT cancels
    @Operation(summary = "Cancel a booking")
    @PutMapping("/cancel/{id}")
    public Booking cancel(@PathVariable Long id) {
        return service.changeStatus(id, "CANCELLED");
    }

    @Operation(summary = "Delete a booking", description = "Only the owner may delete (403 otherwise).")
    @DeleteMapping("/deleteById/{id}")
    public String deleteById(@PathVariable Long id) {
        service.deleteById(id);
        return "Booking deleted successfully";
    }
}
