package AgriTrackBackend.REPORT;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Module 7 — AI Features. Deliberately statistical/rule-based rather than a
 * hosted ML model: a linear trend forecast over the existing 12-month
 * revenue trend (Module 5), and rule-based insights/recommendations derived
 * from data already aggregated by fleetDashboard()/advancedReports()/
 * analytics(). This keeps the feature real and explainable without
 * introducing a new ML dependency or training pipeline, per the "only where
 * it provides real value" instruction.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiInsightsResponse {

    private Long ownerId;

    private List<RevenueForecastPoint> revenueForecast; // next 3 months, linear-trend estimate
    private List<Insight> insights;                       // rule-based business insights
    private List<TractorRecommendation> recommendedTractors; // best next-booking candidates

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueForecastPoint {
        private String period;     // "2026-08"
        private BigDecimal estimatedRevenue;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Insight {
        private String category; // REVENUE / BOOKING / MAINTENANCE / DOCUMENT / DRIVER / CUSTOMER
        private String severity; // INFO / WARNING / SUCCESS
        private String title;
        private String message;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TractorRecommendation {
        private Long tractorId;
        private String model;
        private String registrationNumber;
        private String reason;
        private double score;
    }
}
