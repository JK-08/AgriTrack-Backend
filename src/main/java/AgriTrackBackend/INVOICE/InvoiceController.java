package AgriTrackBackend.INVOICE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invoice")
@CrossOrigin
public class InvoiceController {

    @Autowired
    private InvoiceService service;

    @PostMapping("/create")
    public Invoice create(@RequestBody Invoice invoice) {
        return service.save(invoice);
    }

    @GetMapping("/getAll")
    public List<Invoice> getAll() {
        return service.getAll();
    }

    @GetMapping("/getByOwner/{ownerId}")
    public List<Invoice> getByOwner(@PathVariable Long ownerId) {
        return service.getByOwner(ownerId);
    }

    @GetMapping("/getByCustomer/{customerId}")
    public List<Invoice> getByCustomer(@PathVariable Long customerId) {
        return service.getByCustomer(customerId);
    }

    @GetMapping("/getById/{id}")
    public Invoice getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/update/{id}")
    public Invoice update(@PathVariable Long id, @RequestBody Invoice invoice) {
        return service.update(id, invoice);
    }

    @PutMapping("/status/{id}")
    public Invoice updateStatus(@PathVariable Long id, @RequestParam String status) {
        return service.updateStatus(id, status);
    }

    @DeleteMapping("/deleteById/{id}")
    public String deleteById(@PathVariable Long id) {
        service.deleteById(id);
        return "Invoice deleted successfully";
    }
}
