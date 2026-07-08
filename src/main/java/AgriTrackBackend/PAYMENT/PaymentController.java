package AgriTrackBackend.PAYMENT;

import AgriTrackBackend.COMMON.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payment")
@CrossOrigin
@Tag(name = "Payments", description = "Payment records against invoices/bookings. Accessible by the owner or the paying customer.")
public class PaymentController {

    @Autowired
    private PaymentService service;

    @Operation(summary = "Record a payment", description = "Owner recording cash, or customer paying online. Card/bank details are never stored or logged.")
    @PostMapping("/create")
    public Payment create(@RequestBody Payment payment) {
        return service.save(payment);
    }

    @GetMapping("/getAll")
    public List<Payment> getAll() {
        return service.getAll();
    }

    @GetMapping("/history/{ownerId}")
    public List<Payment> ownerHistory(@PathVariable Long ownerId) {
        return service.getByOwner(ownerId);
    }

    @Operation(summary = "Paged/search/filter/sort payment list", description = "?status/?method/?from/?to filter exactly. Additive.")
    @GetMapping("/search-paged/{ownerId}")
    public PageResponse<Payment> searchPaged(
            @PathVariable Long ownerId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        return service.searchPaged(ownerId, search, status, method, from, to, page, size, sortBy, sortDir);
    }

    @GetMapping("/customerHistory/{customerId}")
    public List<Payment> customerHistory(@PathVariable Long customerId) {
        return service.getByCustomer(customerId);
    }

    @GetMapping("/pending/{ownerId}")
    public List<Payment> pending(@PathVariable Long ownerId) {
        return service.getPending(ownerId);
    }

    @GetMapping("/byInvoice/{invoiceId}")
    public List<Payment> byInvoice(@PathVariable Long invoiceId) {
        return service.getByInvoice(invoiceId);
    }

    @GetMapping("/getById/{id}")
    public Payment getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @Operation(summary = "Update payment status", description = "e.g. PENDING -> SUCCESS/FAILED.")
    @PutMapping("/status/{id}")
    public Payment updateStatus(@PathVariable Long id, @RequestParam String status) {
        return service.updateStatus(id, status);
    }

    @Operation(summary = "Delete a payment record", description = "Only the owner may delete (403 otherwise).")
    @DeleteMapping("/deleteById/{id}")
    public String deleteById(@PathVariable Long id) {
        service.deleteById(id);
        return "Payment deleted successfully";
    }
}
