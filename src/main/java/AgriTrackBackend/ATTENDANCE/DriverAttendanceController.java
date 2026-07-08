package AgriTrackBackend.ATTENDANCE;

import AgriTrackBackend.COMMON.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
@CrossOrigin
@Tag(name = "Driver Attendance", description = "Clock in/out, break time, and leave tracking for drivers. Accessible by the driver themselves or their owner.")
public class DriverAttendanceController {

    @Autowired
    private DriverAttendanceService service;

    @Operation(summary = "Clock in", description = "Creates or reuses today's attendance record. Fails if already clocked in today.")
    @PostMapping("/clock-in/{driverId}")
    public DriverAttendance clockIn(@PathVariable Long driverId) {
        return service.clockIn(driverId);
    }

    @Operation(summary = "Clock out", description = "Computes overtime (worked minutes beyond an 8-hour day, net of breaks).")
    @PostMapping("/clock-out/{driverId}")
    public DriverAttendance clockOut(@PathVariable Long driverId) {
        return service.clockOut(driverId);
    }

    @Operation(summary = "Record a break", description = "Adds to today's accumulated break minutes.")
    @PostMapping("/break/{driverId}")
    public DriverAttendance recordBreak(@PathVariable Long driverId, @RequestParam Integer minutes) {
        return service.recordBreak(driverId, minutes);
    }

    @Operation(summary = "Mark a day as leave", description = "Creates or updates the attendance record for that date with status LEAVE.")
    @PostMapping("/leave")
    public DriverAttendance markLeave(@Valid @RequestBody LeaveRequest request) {
        return service.markLeave(request);
    }

    @Operation(summary = "Paged/search/filter/sort attendance list", description = "?driverId/?status/?from/?to filter exactly/by range.")
    @GetMapping("/search-paged/{ownerId}")
    public PageResponse<DriverAttendance> searchPaged(
            @PathVariable Long ownerId,
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        return service.searchPaged(ownerId, driverId, status, from, to, page, size, sortBy, sortDir);
    }

    @Operation(summary = "Attendance history for a driver in a date range")
    @GetMapping("/by-driver/{driverId}")
    public List<DriverAttendance> byDriver(
            @PathVariable Long driverId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return service.getByDriverAndRange(driverId, from, to);
    }

    @Operation(summary = "Get an attendance record by id")
    @GetMapping("/getById/{id}")
    public DriverAttendance getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @Operation(summary = "Correct an attendance record", description = "Owner-only — drivers cannot retroactively edit their own clock history.")
    @PutMapping("/update/{id}")
    public DriverAttendance update(@PathVariable Long id, @RequestBody DriverAttendance data) {
        return service.update(id, data);
    }

    @Operation(summary = "Delete an attendance record", description = "Only the owning owner may delete (403 otherwise).")
    @DeleteMapping("/deleteById/{id}")
    public String deleteById(@PathVariable Long id) {
        service.deleteById(id);
        return "Attendance record deleted successfully";
    }
}
