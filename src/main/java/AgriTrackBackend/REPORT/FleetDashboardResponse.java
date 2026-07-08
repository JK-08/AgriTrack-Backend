package AgriTrackBackend.REPORT;

import AgriTrackBackend.AUDIT.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Single aggregate payload for the Owner's Fleet Dashboard. Deliberately a
 * plain response DTO (not an entity) — it stitches together read-only data
 * already owned by WorkRecord/Payment/Booking/Tractor/Driver/MaintenanceLog/
 * AuditLog services, it does not introduce any new persisted state.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FleetDashboardResponse {

    private Long ownerId;

    // Revenue KPIs
    private BigDecimal todayRevenue;
    private BigDecimal monthRevenue;
    private BigDecimal totalRevenue;

    // Job / booking KPIs
    private long activeJobsCount;
    private long todaysBookingsCount;
    private long upcomingJobsCount;

    // Utilization
    private double tractorUtilizationPercent;
    private int totalTractors;
    private int busyTractors;

    private double driverUtilizationPercent;
    private int totalDrivers;
    private int busyDrivers;

    // Payments
    private BigDecimal pendingPaymentsAmount;
    private long pendingPaymentsCount;

    // Charts
    private List<MonthlyRevenuePoint> monthlyRevenueChart;

    // Maintenance
    private List<UpcomingMaintenanceItem> upcomingMaintenance;

    // Activity feed
    private List<AuditLog> recentActivities;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenuePoint {
        private String month; // "2026-07"
        private BigDecimal revenue;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpcomingMaintenanceItem {
        private Long tractorId;
        private String model;
        private String registrationNumber;
        private String lastMaintenanceDate; // ISO date string, or null if never serviced
        private long daysSinceLastMaintenance;
    }
}
