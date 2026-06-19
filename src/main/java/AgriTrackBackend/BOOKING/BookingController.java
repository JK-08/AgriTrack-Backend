package AgriTrackBackend.BOOKING;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/booking")
@CrossOrigin
public class BookingController {

    @Autowired
    private BookingService service;

    // ✅ CLIENT requests a service
    @PostMapping("/create")
    public Booking create(@RequestBody Booking booking) {
        return service.save(booking);
    }

    @GetMapping("/getAll")
    public List<Booking> getAll() {
        return service.getAll();
    }

    @GetMapping("/getByOwner/{ownerId}")
    public List<Booking> getByOwner(@PathVariable Long ownerId) {
        return service.getByOwner(ownerId);
    }

    @GetMapping("/getByClient/{clientId}")
    public List<Booking> getByClient(@PathVariable Long clientId) {
        return service.getByClient(clientId);
    }

    @GetMapping("/pending/{ownerId}")
    public List<Booking> pending(@PathVariable Long ownerId) {
        return service.getPending(ownerId);
    }

    @GetMapping("/getById/{id}")
    public Booking getById(@PathVariable Long id) {
        return service.getById(id);
    }

    // ✅ OWNER accepts
    @PutMapping("/accept/{id}")
    public Booking accept(@PathVariable Long id) {
        return service.changeStatus(id, "ACCEPTED");
    }

    // ✅ OWNER rejects
    @PutMapping("/reject/{id}")
    public Booking reject(@PathVariable Long id) {
        return service.changeStatus(id, "REJECTED");
    }

    // ✅ OWNER marks complete
    @PutMapping("/complete/{id}")
    public Booking complete(@PathVariable Long id) {
        return service.changeStatus(id, "COMPLETED");
    }

    // ✅ CLIENT cancels
    @PutMapping("/cancel/{id}")
    public Booking cancel(@PathVariable Long id) {
        return service.changeStatus(id, "CANCELLED");
    }

    @DeleteMapping("/deleteById/{id}")
    public String deleteById(@PathVariable Long id) {
        service.deleteById(id);
        return "Booking deleted successfully";
    }
}
