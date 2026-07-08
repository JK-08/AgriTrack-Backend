package AgriTrackBackend.HEALTH;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Cold-start probe for the frontend's boot gate (see ServerBootGate.jsx).
 * Render's free tier spins the backend down after inactivity, so the app
 * polls this endpoint before doing anything else. Deliberately trivial —
 * no DB/dependency checks — so it responds the instant the JVM is up,
 * rather than waiting on a datasource connection that may itself still be
 * warming up.
 */
@RestController
@Tag(name = "Health", description = "Lightweight liveness probe, no authentication required.")
public class HealthController {

    @Operation(summary = "Health check", description = "Always returns 200 with {\"status\":\"UP\"} once the "
            + "application context has finished starting. Used by the frontend to detect and wait out a Render "
            + "cold start before making any real API calls.")
    @GetMapping("/api/v1/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
