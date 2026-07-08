package AgriTrackBackend.DRIVER;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/location")
@CrossOrigin
@Tag(name = "Driver Location", description = "Live GPS breadcrumbs posted by the driver app while a job is running. No list endpoint — only latest-location lookups.")
public class DriverLocationController {

    @Autowired
    private DriverLocationService service;

    // ✅ DRIVER app posts a GPS breadcrumb while a job is running
    @Operation(summary = "Post a GPS breadcrumb", description = "Driver Event — audited with driverId/bookingId metadata only, never raw coordinates spam.")
    @PostMapping("/update")
    public DriverLocation update(@RequestBody DriverLocation location) {
        return service.record(location);
    }

    // ✅ latest known location for a driver (owner/driver app)
    @Operation(summary = "Latest location for a driver")
    @GetMapping("/latestByDriver/{driverId}")
    public DriverLocation latestByDriver(@PathVariable Long driverId) {
        return service.latestByDriver(driverId);
    }

    // ✅ latest known location for a booking (client/farmer app)
    @Operation(summary = "Latest location for a booking")
    @GetMapping("/latestByBooking/{bookingId}")
    public DriverLocation latestByBooking(@PathVariable Long bookingId) {
        return service.latestByBooking(bookingId);
    }
}
