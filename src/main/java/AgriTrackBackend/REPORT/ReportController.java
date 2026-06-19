package AgriTrackBackend.REPORT;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/report")
@CrossOrigin
public class ReportController {

    @Autowired
    private ReportService service;

    // ✅ Owner dashboard stats
    @GetMapping("/owner/{ownerId}")
    public Map<String, Object> ownerSummary(@PathVariable Long ownerId) {
        return service.ownerSummary(ownerId);
    }

    // ✅ Revenue time-series for analytics charts
    @GetMapping("/revenue/{ownerId}")
    public Map<String, Object> revenue(@PathVariable Long ownerId) {
        return service.revenueByDate(ownerId);
    }
}
