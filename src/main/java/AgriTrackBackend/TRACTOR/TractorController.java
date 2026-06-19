package AgriTrackBackend.TRACTOR;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tractor")
@CrossOrigin
public class TractorController {

    @Autowired
    private TractorService service;

    @PostMapping("/create")
    public Tractor create(@RequestBody Tractor tractor) {
        return service.save(tractor);
    }

    @GetMapping("/getAll")
    public List<Tractor> getAll() {
        return service.getAll();
    }

    @GetMapping("/getByOwner/{ownerId}")
    public List<Tractor> getByOwner(@PathVariable Long ownerId) {
        return service.getByOwner(ownerId);
    }

    @GetMapping("/available/{ownerId}")
    public List<Tractor> available(@PathVariable Long ownerId) {
        return service.getAvailable(ownerId);
    }

    @GetMapping("/getById/{id}")
    public Tractor getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/update/{id}")
    public Tractor update(@PathVariable Long id, @RequestBody Tractor tractor) {
        return service.update(id, tractor);
    }

    @PutMapping("/status/{id}")
    public Tractor updateStatus(@PathVariable Long id, @RequestParam String status) {
        return service.updateStatus(id, status);
    }

    @DeleteMapping("/deleteById/{id}")
    public String deleteById(@PathVariable Long id) {
        service.deleteById(id);
        return "Tractor deleted successfully";
    }
}
