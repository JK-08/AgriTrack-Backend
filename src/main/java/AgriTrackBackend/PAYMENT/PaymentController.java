package AgriTrackBackend.PAYMENT;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payment")
@CrossOrigin
public class PaymentController {

    @Autowired
    private PaymentService service;

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

    @PutMapping("/status/{id}")
    public Payment updateStatus(@PathVariable Long id, @RequestParam String status) {
        return service.updateStatus(id, status);
    }

    @DeleteMapping("/deleteById/{id}")
    public String deleteById(@PathVariable Long id) {
        service.deleteById(id);
        return "Payment deleted successfully";
    }
}
