package AgriTrackBackend.WORK;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/work")
@CrossOrigin
public class WorkRecordController {

    @Autowired
    private WorkRecordService service;

    // ✅ TIMER: START
    @PostMapping("/start")
    public WorkRecord start(@RequestBody StartWorkRequest request) {
        return service.start(request);
    }

    // ✅ TIMER: PAUSE
    @PutMapping("/pause/{id}")
    public WorkRecord pause(@PathVariable Long id) {
        return service.pause(id);
    }

    // ✅ TIMER: RESUME
    @PutMapping("/resume/{id}")
    public WorkRecord resume(@PathVariable Long id) {
        return service.resume(id);
    }

    // ✅ TIMER: STOP / COMPLETE
    @PutMapping("/stop/{id}")
    public WorkRecord stop(@PathVariable Long id,
                           @RequestParam(required = false) BigDecimal extraCharges) {
        return service.stop(id, extraCharges);
    }

    // ✅ MANUAL ENTRY
    @PostMapping("/manual")
    public WorkRecord manual(@RequestBody WorkRecord workRecord) {
        return service.saveManual(workRecord);
    }

    @GetMapping("/getAll")
    public List<WorkRecord> getAll() {
        return service.getAll();
    }

    @GetMapping("/getByOwner/{ownerId}")
    public List<WorkRecord> getByOwner(@PathVariable Long ownerId) {
        return service.getByOwner(ownerId);
    }

    @GetMapping("/getByCustomer/{customerId}")
    public List<WorkRecord> getByCustomer(@PathVariable Long customerId) {
        return service.getByCustomer(customerId);
    }

    @GetMapping("/getById/{id}")
    public WorkRecord getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @DeleteMapping("/deleteById/{id}")
    public String deleteById(@PathVariable Long id) {
        service.deleteById(id);
        return "Work record deleted successfully";
    }
}
