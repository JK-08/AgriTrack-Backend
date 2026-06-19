package AgriTrackBackend.RATING;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rating")
@CrossOrigin
public class RatingController {

    @Autowired
    private RatingService service;

    @PostMapping("/create")
    public Rating create(@RequestBody Rating rating) {
        return service.save(rating);
    }

    @GetMapping("/getAll")
    public List<Rating> getAll() {
        return service.getAll();
    }

    @GetMapping("/getByOwner/{ownerId}")
    public List<Rating> getByOwner(@PathVariable Long ownerId) {
        return service.getByOwner(ownerId);
    }

    @GetMapping("/getByClient/{clientId}")
    public List<Rating> getByClient(@PathVariable Long clientId) {
        return service.getByClient(clientId);
    }

    @GetMapping("/average/{ownerId}")
    public Map<String, Object> average(@PathVariable Long ownerId) {
        Map<String, Object> res = new HashMap<>();
        res.put("ownerId", ownerId);
        res.put("averageRating", service.getAverageForOwner(ownerId));
        return res;
    }

    @GetMapping("/getById/{id}")
    public Rating getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @DeleteMapping("/deleteById/{id}")
    public String deleteById(@PathVariable Long id) {
        service.deleteById(id);
        return "Rating deleted successfully";
    }
}
