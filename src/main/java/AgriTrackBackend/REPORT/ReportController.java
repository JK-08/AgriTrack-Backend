package AgriTrackBackend.REPORT;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/report")
@CrossOrigin
@Tag(name = "Reports", description = "Read-only dashboard/analytics aggregates for an owner. Ownership-scoped, no pagination (aggregate summaries, not lists).")
public class ReportController {

    @Autowired
    private ReportService service;

    // ✅ Owner dashboard stats
    @Operation(summary = "Owner dashboard summary", description = "Aggregate counts/totals (bookings, revenue, fleet status, etc.) for the owner's dashboard.")
    @GetMapping("/owner/{ownerId}")
    public Map<String, Object> ownerSummary(@PathVariable Long ownerId) {
        return service.ownerSummary(ownerId);
    }

    // ✅ Revenue time-series for analytics charts
    @Operation(summary = "Revenue time-series", description = "Revenue grouped by date, for analytics charts.")
    @GetMapping("/revenue/{ownerId}")
    public Map<String, Object> revenue(@PathVariable Long ownerId) {
        return service.revenueByDate(ownerId);
    }

    // ✅ Fleet Dashboard — single aggregate payload for the owner's home dashboard
    @Operation(summary = "Fleet Dashboard", description = "Aggregate KPIs: revenue (today/month/total), active jobs, "
            + "today's bookings, upcoming jobs, tractor/driver utilization, 6-month revenue chart, pending payments, "
            + "upcoming maintenance (tractors unserviced 90+ days), and the 10 most recent audited activities.")
    @GetMapping("/fleet-dashboard/{ownerId}")
    public FleetDashboardResponse fleetDashboard(@PathVariable Long ownerId) {
        return service.fleetDashboard(ownerId);
    }

    // ✅ Module 4 — Advanced Reports (driver performance, tractor utilization,
    // booking/customer/payment analytics, maintenance cost). Revenue/expense/
    // P&L stay on fleet-dashboard and /expense/profit-loss respectively.
    @Operation(summary = "Advanced reports", description = "Driver performance, tractor utilization, booking analytics "
            + "(status/month/peak-hour breakdown), top 10 customers by spend, payment analytics (by status/method), "
            + "and maintenance cost per tractor. Defaults to the last 6 months when ?from/?to are omitted.")
    @GetMapping("/advanced/{ownerId}")
    public AdvancedReportsResponse advancedReports(
            @PathVariable Long ownerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return service.advancedReports(ownerId, from, to);
    }

    // ✅ Module 5 — Advanced Analytics (12-month trends + seasonal pattern).
    // Top customers/driver performance stay on /report/advanced to avoid
    // duplicating that aggregation.
    @Operation(summary = "Advanced analytics", description = "12-month rolling trends for revenue, bookings, customer "
            + "growth, and tractor usage hours; all-time driver productivity (jobs per hour); month-of-year seasonal "
            + "booking pattern; and all-time peak booking hours.")
    @GetMapping("/analytics/{ownerId}")
    public AnalyticsResponse analytics(@PathVariable Long ownerId) {
        return service.analytics(ownerId);
    }

    // ✅ Module 7 — AI Features. Statistical (linear-trend) revenue forecast,
    // rule-based business insights, and idle-tractor booking recommendations.
    // Deliberately not a hosted ML model — see AiInsightsResponse javadoc.
    @Operation(summary = "AI business insights", description = "3-month linear-trend revenue forecast, rule-based "
            + "insights (revenue trend, idle fleet, pending bookings, expiring documents, top driver), and up to 5 "
            + "recommended tractors for the next booking (available, under-utilized, high-earning).")
    @GetMapping("/insights/{ownerId}")
    public AiInsightsResponse insights(@PathVariable Long ownerId) {
        return service.insights(ownerId);
    }
}
