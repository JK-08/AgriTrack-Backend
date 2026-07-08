package AgriTrackBackend.EXPENSE;

import AgriTrackBackend.CLOUDINARY.CloudinaryService;
import AgriTrackBackend.COMMON.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/expense")
@CrossOrigin
@Tag(name = "Expenses", description = "Owner business expenses (fuel, repairs, spares, insurance, driver salary, EMI, taxes, misc) feeding Profit & Loss.")
public class ExpenseController {

    @Autowired
    private ExpenseService service;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Operation(summary = "Upload a bill/receipt image", description = "Returns a URL to pass as billUrl on create/update. Separate from create so the same bill can be re-attached on edit without re-uploading.")
    @PostMapping("/upload-bill")
    public Map<String, String> uploadBill(@RequestParam MultipartFile file) {
        return Map.of("url", cloudinaryService.uploadImage(file));
    }

    @Operation(summary = "Record an expense", description = "ownerId is always taken from the caller's JWT. category must be one of FUEL, REPAIRS, SPARE_PARTS, INSURANCE, DRIVER_SALARY, EMI, TAXES, MISC.")
    @PostMapping("/create")
    public Expense create(@Valid @RequestBody ExpenseRequest request) {
        return service.create(request);
    }

    @Operation(summary = "Paged/search/filter/sort expense list", description = "?search matches vendor/description; ?category/?tractorId/?from/?to filter exactly/by range.")
    @GetMapping("/search-paged/{ownerId}")
    public PageResponse<Expense> searchPaged(
            @PathVariable Long ownerId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long tractorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        return service.searchPaged(ownerId, search, category, tractorId, from, to, page, size, sortBy, sortDir);
    }

    @Operation(summary = "Get an expense by id")
    @GetMapping("/getById/{id}")
    public Expense getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @Operation(summary = "Update an expense", description = "Only the owning owner may update (403 otherwise).")
    @PutMapping("/update/{id}")
    public Expense update(@PathVariable Long id, @Valid @RequestBody ExpenseRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Delete an expense", description = "Only the owning owner may delete (403 otherwise).")
    @DeleteMapping("/deleteById/{id}")
    public String deleteById(@PathVariable Long id) {
        service.deleteById(id);
        return "Expense deleted successfully";
    }

    @Operation(summary = "Profit & Loss + category/monthly breakdown", description = "Defaults to the last 6 months when ?from/?to are omitted. Revenue is derived from successful payments — never duplicated logic, composed from the existing Payment data.")
    @GetMapping("/profit-loss/{ownerId}")
    public ProfitLossResponse profitAndLoss(
            @PathVariable Long ownerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return service.profitAndLoss(ownerId, from, to);
    }
}
