package AgriTrackBackend.RATE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rate")
@CrossOrigin
public class RateController {

    @Autowired
    private RateService service;

    @PostMapping("/create")
    public Rate create(@RequestBody Rate rate) {
        return service.save(rate);
    }

    @GetMapping("/getAll")
    public List<Rate> getAll() {
        return service.getAll();
    }

    @GetMapping("/getByOwner/{ownerId}")
    public List<Rate> getByOwner(@PathVariable Long ownerId) {
        return service.getByOwner(ownerId);
    }

    @GetMapping("/active/{ownerId}")
    public List<Rate> active(@PathVariable Long ownerId) {
        return service.getActiveByOwner(ownerId);
    }

    @GetMapping("/getById/{id}")
    public Rate getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/update/{id}")
    public Rate update(@PathVariable Long id, @RequestBody Rate rate) {
        return service.update(id, rate);
    }

    @DeleteMapping("/deleteById/{id}")
    public String deleteById(@PathVariable Long id) {
        service.deleteById(id);
        return "Rate deleted successfully";
    }
}
