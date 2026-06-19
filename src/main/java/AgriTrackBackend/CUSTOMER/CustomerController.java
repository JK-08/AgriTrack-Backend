package AgriTrackBackend.CUSTOMER;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer")
@CrossOrigin
public class CustomerController {

    @Autowired
    private CustomerService service;

    // ✅ CREATE
    @PostMapping("/create")
    public Customer create(@RequestBody Customer customer) {
        return service.save(customer);
    }

    // ✅ GET ALL
    @GetMapping("/getAll")
    public List<Customer> getAll() {
        return service.getAll();
    }

    // ✅ GET ALL FOR ONE OWNER
    @GetMapping("/getByOwner/{ownerId}")
    public List<Customer> getByOwner(@PathVariable Long ownerId) {
        return service.getByOwner(ownerId);
    }

    // ✅ SEARCH BY NAME WITHIN OWNER
    @GetMapping("/search/{ownerId}")
    public List<Customer> search(@PathVariable Long ownerId, @RequestParam String name) {
        return service.searchByOwner(ownerId, name);
    }

    // ✅ FILTER NEW / EXISTING
    @GetMapping("/filter/{ownerId}")
    public List<Customer> filter(@PathVariable Long ownerId, @RequestParam String type) {
        return service.getByOwnerAndType(ownerId, type);
    }

    // ✅ GET BY ID
    @GetMapping("/getById/{id}")
    public Customer getById(@PathVariable Long id) {
        return service.getById(id);
    }

    // ✅ UPDATE
    @PutMapping("/update/{id}")
    public Customer update(@PathVariable Long id, @RequestBody Customer customer) {
        return service.update(id, customer);
    }

    // ✅ DELETE
    @DeleteMapping("/deleteById/{id}")
    public String deleteById(@PathVariable Long id) {
        service.deleteById(id);
        return "Customer deleted successfully";
    }
}
