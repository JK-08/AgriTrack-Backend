package AgriTrackBackend.PAYROLL;

import AgriTrackBackend.COMMON.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payroll")
@CrossOrigin
@Tag(name = "Driver Payroll", description = "Monthly salary calculation from attendance (base salary, overtime, incentives, penalties). Owner-only.")
public class DriverPayrollController {

    @Autowired
    private DriverPayrollService service;

    @Operation(summary = "Generate/regenerate a driver's payroll for a month",
            description = "Computes present/absent/leave days and overtime from DRIVER_ATTENDANCE, applies the driver's monthlySalary, and adds incentives minus penalties. Idempotent per driver+month — calling again recalculates.")
    @PostMapping("/generate")
    public DriverPayroll generate(@Valid @RequestBody GeneratePayrollRequest request) {
        return service.generate(request);
    }

    @Operation(summary = "Paged/search/filter/sort payroll list", description = "?driverId/?status/?month filter exactly.")
    @GetMapping("/search-paged/{ownerId}")
    public PageResponse<DriverPayroll> searchPaged(
            @PathVariable Long ownerId,
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        return service.searchPaged(ownerId, driverId, status, month, page, size, sortBy, sortDir);
    }

    @Operation(summary = "Get a payroll record by id")
    @GetMapping("/getById/{id}")
    public DriverPayroll getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @Operation(summary = "Mark payroll as paid", description = "Payment event — audited as a status change.")
    @PutMapping("/mark-paid/{id}")
    public DriverPayroll markPaid(@PathVariable Long id) {
        return service.markPaid(id);
    }

    @Operation(summary = "Delete a payroll record", description = "Only the owning owner may delete (403 otherwise).")
    @DeleteMapping("/deleteById/{id}")
    public String deleteById(@PathVariable Long id) {
        service.deleteById(id);
        return "Payroll record deleted successfully";
    }
}
