package AgriTrackBackend.REPORT;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Module 5 — Advanced Analytics. Trend-focused aggregate payload, distinct
 * from AdvancedReportsResponse (Module 4, point-in-range breakdown). This
 * response is time-series oriented: 12-month rolling trends plus a
 * month-of-year seasonal pattern. It deliberately does not re-derive top
 * customers or driver job counts — those already exist in
 * AdvancedReportsResponse (topCustomers, driverPerformance) and the frontend
 * composes both calls instead of duplicating that aggregation here.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponse {

    private Long ownerId;

    private List<TrendPoint> revenueTrend;       // last 12 months
    private List<TrendPoint> bookingTrend;        // last 12 months, booking count
    private List<TrendPoint> customerGrowthTrend; // last 12 months, new customers
    private List<TrendPoint> tractorUsageTrend;    // last 12 months, hours used

    private List<DriverProductivityPoint> driverProductivity; // jobs/hour by driver, all-time

    private Map<String, Long> seasonalBookingPattern; // "01".."12" -> booking count across all years
    private Map<Integer, Long> peakBookingHours;       // hour of day -> booking count, all-time

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String period; // "2026-07"
        private BigDecimal value;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverProductivityPoint {
        private Long driverId;
        private String licenseNumber;
        private long completedJobs;
        private double jobsPerHour; // completedJobs / (totalMinutesWorked / 60), 0 if no minutes
    }
}
