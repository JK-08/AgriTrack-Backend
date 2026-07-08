package AgriTrackBackend.REPORT;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Single aggregate payload covering every report category in Module 4
 * (Driver Performance, Tractor Utilization, Booking Analytics, Customer
 * Analytics, Payment Analytics, Maintenance Report). Composed entirely from
 * data already owned by WorkRecord/Booking/Payment/Customer/MaintenanceLog/
 * Tractor/Driver repositories — no new tables, no duplicated aggregation
 * (revenue/expense/P&L already live in ReportService.fleetDashboard and
 * ExpenseService.profitAndLoss and are intentionally not repeated here).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdvancedReportsResponse {

    private Long ownerId;
    private String from;
    private String to;

    private List<DriverPerformance> driverPerformance;
    private List<TractorUtilization> tractorUtilization;
    private BookingAnalytics bookingAnalytics;
    private List<TopCustomer> topCustomers;
    private PaymentAnalytics paymentAnalytics;
    private List<MaintenanceCost> maintenanceCosts;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class DriverPerformance {
        private Long driverId;
        private String licenseNumber;
        private long completedJobs;
        private long totalMinutesWorked;
        private BigDecimal revenueGenerated;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class TractorUtilization {
        private Long tractorId;
        private String model;
        private String registrationNumber;
        private long completedJobs;
        private long totalMinutesUsed;
        private BigDecimal revenueGenerated;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class BookingAnalytics {
        private long totalBookings;
        private Map<String, Long> byStatus;
        private Map<String, Long> byMonth;
        private Map<Integer, Long> byHourOfDay; // peak booking hours
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class TopCustomer {
        private Long customerId;
        private String name;
        private long bookingCount;
        private BigDecimal totalSpent;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class PaymentAnalytics {
        private Map<String, Long> countByStatus;
        private Map<String, BigDecimal> amountByMethod;
        private double successRatePercent;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class MaintenanceCost {
        private Long tractorId;
        private String model;
        private long serviceCount;
        private BigDecimal totalCost;
    }
}
