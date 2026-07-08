package AgriTrackBackend.CUSTOMER;

import AgriTrackBackend.COMMON.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer")
@CrossOrigin
@Tag(name = "Customers", description = "An owner's customer/farmer records. All list endpoints are ownership-scoped.")
public class CustomerController {

    @Autowired
    private CustomerService service;

    @Operation(summary = "Create a customer", description = "ownerId is always taken from the caller's JWT, never the request body.")
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

    @Operation(summary = "Paged/search/filter/sort customer list",
            description = "?search matches name/village/mobile (case-insensitive). ?customerType filters exactly. "
                    + "?sortBy defaults to createdAt, ?sortDir defaults to desc. Additive — existing "
                    + "getAll/getByOwner/search/filter endpoints above are unchanged.")
    @GetMapping("/search-paged/{ownerId}")
    public PageResponse<Customer> searchPaged(
            @PathVariable Long ownerId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String customerType,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        return service.searchPaged(ownerId, search, customerType, page, size, sortBy, sortDir);
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
