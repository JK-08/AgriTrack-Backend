package AgriTrackBackend.REPORT;

import AgriTrackBackend.AUDIT.AuditLog;
import AgriTrackBackend.AUDIT.AuditLogRepository;
import AgriTrackBackend.BOOKING.Booking;
import AgriTrackBackend.BOOKING.BookingRepository;
import AgriTrackBackend.CUSTOMER.Customer;
import AgriTrackBackend.CUSTOMER.CustomerRepository;
import AgriTrackBackend.DOCUMENT.TractorDocument;
import AgriTrackBackend.DOCUMENT.TractorDocumentRepository;
import AgriTrackBackend.DRIVER.Driver;
import AgriTrackBackend.DRIVER.DriverRepository;
import AgriTrackBackend.MAINTENANCE.MaintenanceLog;
import AgriTrackBackend.MAINTENANCE.MaintenanceLogRepository;
import AgriTrackBackend.PAYMENT.Payment;
import AgriTrackBackend.PAYMENT.PaymentRepository;
import AgriTrackBackend.RATING.RatingService;
import AgriTrackBackend.SECURITY.CurrentUser;
import AgriTrackBackend.TRACTOR.Tractor;
import AgriTrackBackend.TRACTOR.TractorRepository;
import AgriTrackBackend.WORK.WorkRecord;
import AgriTrackBackend.WORK.WorkRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ReportService {

    @Autowired
    private WorkRecordRepository workRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TractorRepository tractorRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private MaintenanceLogRepository maintenanceLogRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private RatingService ratingService;

    @Autowired
    private TractorDocumentRepository tractorDocumentRepository;

    // Tractors not serviced within this window surface as "upcoming maintenance" —
    // there's no dedicated maintenance-schedule table yet, so this is a
    // reasonable heuristic reusing existing MaintenanceLog history.
    private static final int MAINTENANCE_DUE_AFTER_DAYS = 90;

    // Module 5 — trailing window for analytics()'s revenue/booking/customer/usage trends.
    private static final int TREND_MONTHS = 12;

    // Module 7 — how many months ahead insights() forecasts revenue.
    private static final int FORECAST_MONTHS = 3;

    // ✅ Owner dashboard summary
    public Map<String, Object> ownerSummary(Long ownerId) {
        CurrentUser.requireSelf(ownerId);
        Map<String, Object> summary = new HashMap<>();

        List<WorkRecord> works = workRepository.findByOwnerIdOrderByWorkIdDesc(ownerId);
        List<Payment> payments = paymentRepository.findByOwnerIdOrderByPaymentIdDesc(ownerId);
        List<Booking> bookings = bookingRepository.findByOwnerIdOrderByBookingIdDesc(ownerId);

        int totalCustomers = customerRepository.findByOwnerId(ownerId).size();

        long completedWorks = works.stream()
                .filter(w -> "COMPLETED".equalsIgnoreCase(w.getStatus())).count();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Payment p : payments) {
            if ("SUCCESS".equalsIgnoreCase(p.getPaymentStatus()) && p.getAmount() != null) {
                totalRevenue = totalRevenue.add(p.getAmount());
            }
        }

        BigDecimal pendingDue = BigDecimal.ZERO;
        for (Payment p : payments) {
            if ("PENDING".equalsIgnoreCase(p.getPaymentStatus()) && p.getAmount() != null) {
                pendingDue = pendingDue.add(p.getAmount());
            }
        }

        long pendingBookings = bookings.stream()
                .filter(b -> "PENDING".equalsIgnoreCase(b.getStatus())).count();

        summary.put("ownerId", ownerId);
        summary.put("totalCustomers", totalCustomers);
        summary.put("totalWorks", works.size());
        summary.put("completedWorks", completedWorks);
        summary.put("totalBookings", bookings.size());
        summary.put("pendingBookings", pendingBookings);
        summary.put("totalRevenue", totalRevenue);
        summary.put("pendingDue", pendingDue);
        summary.put("averageRating", ratingService.getAverageForOwner(ownerId));
        return summary;
    }

    // ✅ Revenue grouped by work date (simple time-series for charts)
    public Map<String, Object> revenueByDate(Long ownerId) {
        CurrentUser.requireSelf(ownerId);
        List<WorkRecord> works = workRepository.findByOwnerIdOrderByWorkIdDesc(ownerId);
        Map<LocalDate, BigDecimal> grouped = new HashMap<>();
        for (WorkRecord w : works) {
            if (w.getWorkDate() != null && w.getAmount() != null) {
                grouped.merge(w.getWorkDate(), w.getAmount(), BigDecimal::add);
            }
        }
        Map<String, Object> result = new HashMap<>();
        Map<String, BigDecimal> series = new HashMap<>();
        grouped.forEach((k, v) -> series.put(k.toString(), v));
        result.put("ownerId", ownerId);
        result.put("revenueByDate", series);
        return result;
    }

    // ✅ Fleet Dashboard — single aggregate read stitched together from data
    // already owned by other repositories. No new tables, no duplicated
    // business logic — just composition + read-only aggregation.
    public FleetDashboardResponse fleetDashboard(Long ownerId) {
        CurrentUser.requireSelf(ownerId);

        List<WorkRecord> works = workRepository.findByOwnerIdOrderByWorkIdDesc(ownerId);
        List<Payment> payments = paymentRepository.findByOwnerIdOrderByPaymentIdDesc(ownerId);
        List<Booking> bookings = bookingRepository.findByOwnerIdOrderByBookingIdDesc(ownerId);
        List<Tractor> tractors = tractorRepository.findByOwnerId(ownerId);
        List<Driver> drivers = driverRepository.findByOwnerId(ownerId);
        List<MaintenanceLog> maintenanceLogs = maintenanceLogRepository.findByOwnerIdOrderByMaintenanceIdDesc(ownerId);

        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();

        // --- Revenue ---
        BigDecimal todayRevenue = BigDecimal.ZERO;
        BigDecimal monthRevenue = BigDecimal.ZERO;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Payment p : payments) {
            if (!"SUCCESS".equalsIgnoreCase(p.getPaymentStatus()) || p.getAmount() == null || p.getPaymentDate() == null) {
                continue;
            }
            totalRevenue = totalRevenue.add(p.getAmount());
            if (!p.getPaymentDate().isBefore(startOfMonth)) {
                monthRevenue = monthRevenue.add(p.getAmount());
            }
            if (!p.getPaymentDate().isBefore(startOfToday)) {
                todayRevenue = todayRevenue.add(p.getAmount());
            }
        }

        // --- Jobs / bookings ---
        long activeJobs = works.stream()
                .filter(w -> "RUNNING".equalsIgnoreCase(w.getStatus()) || "PAUSED".equalsIgnoreCase(w.getStatus()))
                .count();

        long todaysBookings = bookings.stream()
                .filter(b -> b.getRequestedDate() != null && b.getRequestedDate().toLocalDate().isEqual(today))
                .count();

        long upcomingJobs = bookings.stream()
                .filter(b -> b.getRequestedDate() != null && b.getRequestedDate().isAfter(LocalDateTime.now()))
                .filter(b -> "ACCEPTED".equalsIgnoreCase(b.getStatus()) || "PENDING".equalsIgnoreCase(b.getStatus()))
                .count();

        // --- Utilization ---
        int totalTractors = tractors.size();
        int busyTractors = (int) tractors.stream()
                .filter(t -> "BUSY".equalsIgnoreCase(t.getStatus()) || "IN_USE".equalsIgnoreCase(t.getStatus()))
                .count();
        double tractorUtilization = totalTractors == 0 ? 0.0
                : round1((busyTractors * 100.0) / totalTractors);

        int totalDrivers = drivers.size();
        int busyDrivers = (int) drivers.stream()
                .filter(d -> Boolean.FALSE.equals(d.getIsAvailable()))
                .count();
        double driverUtilization = totalDrivers == 0 ? 0.0
                : round1((busyDrivers * 100.0) / totalDrivers);

        // --- Pending payments ---
        BigDecimal pendingAmount = BigDecimal.ZERO;
        long pendingCount = 0;
        for (Payment p : payments) {
            if ("PENDING".equalsIgnoreCase(p.getPaymentStatus())) {
                pendingCount++;
                if (p.getAmount() != null) pendingAmount = pendingAmount.add(p.getAmount());
            }
        }

        // --- Monthly revenue chart (last 6 months, oldest first) ---
        DateTimeFormatter monthKeyFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, BigDecimal> monthly = new TreeMap<>();
        YearMonth cursor = YearMonth.from(today).minusMonths(5);
        for (int i = 0; i < 6; i++) {
            monthly.put(cursor.format(monthKeyFmt), BigDecimal.ZERO);
            cursor = cursor.plusMonths(1);
        }
        for (Payment p : payments) {
            if (!"SUCCESS".equalsIgnoreCase(p.getPaymentStatus()) || p.getAmount() == null || p.getPaymentDate() == null) {
                continue;
            }
            String key = p.getPaymentDate().format(monthKeyFmt);
            if (monthly.containsKey(key)) {
                monthly.merge(key, p.getAmount(), BigDecimal::add);
            }
        }
        List<FleetDashboardResponse.MonthlyRevenuePoint> monthlyChart = new ArrayList<>();
        monthly.forEach((k, v) -> monthlyChart.add(new FleetDashboardResponse.MonthlyRevenuePoint(k, v)));

        // --- Upcoming maintenance: tractors not serviced within the window ---
        Map<Long, LocalDate> lastServiceByTractor = new HashMap<>();
        for (MaintenanceLog log : maintenanceLogs) {
            if (log.getMaintenanceDate() == null) continue;
            lastServiceByTractor.merge(log.getTractorId(), log.getMaintenanceDate(),
                    (a, b) -> a.isAfter(b) ? a : b);
        }
        List<FleetDashboardResponse.UpcomingMaintenanceItem> upcomingMaintenance = new ArrayList<>();
        for (Tractor t : tractors) {
            LocalDate lastService = lastServiceByTractor.get(t.getTractorId());
            long daysSince = lastService == null ? Long.MAX_VALUE : ChronoUnit.DAYS.between(lastService, today);
            if (lastService == null || daysSince >= MAINTENANCE_DUE_AFTER_DAYS) {
                upcomingMaintenance.add(new FleetDashboardResponse.UpcomingMaintenanceItem(
                        t.getTractorId(), t.getModel(), t.getRegistrationNumber(),
                        lastService == null ? null : lastService.toString(),
                        lastService == null ? -1 : daysSince));
            }
        }
        upcomingMaintenance.sort(Comparator.comparingLong(
                FleetDashboardResponse.UpcomingMaintenanceItem::getDaysSinceLastMaintenance).reversed());

        // --- Recent activity (last 10 audited events for this owner) ---
        List<AuditLog> recentActivities = auditLogRepository
                .search(ownerId, null, null, null, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent();

        FleetDashboardResponse response = new FleetDashboardResponse();
        response.setOwnerId(ownerId);
        response.setTodayRevenue(todayRevenue);
        response.setMonthRevenue(monthRevenue);
        response.setTotalRevenue(totalRevenue);
        response.setActiveJobsCount(activeJobs);
        response.setTodaysBookingsCount(todaysBookings);
        response.setUpcomingJobsCount(upcomingJobs);
        response.setTractorUtilizationPercent(tractorUtilization);
        response.setTotalTractors(totalTractors);
        response.setBusyTractors(busyTractors);
        response.setDriverUtilizationPercent(driverUtilization);
        response.setTotalDrivers(totalDrivers);
        response.setBusyDrivers(busyDrivers);
        response.setPendingPaymentsAmount(pendingAmount);
        response.setPendingPaymentsCount(pendingCount);
        response.setMonthlyRevenueChart(monthlyChart);
        response.setUpcomingMaintenance(upcomingMaintenance);
        response.setRecentActivities(recentActivities);
        return response;
    }

    private double round1(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    // ✅ Module 4 — Advanced Reports. Composes existing repositories; revenue/
    // expense/P&L already live in fleetDashboard() and ExpenseService, so
    // they are deliberately not repeated here.
    public AdvancedReportsResponse advancedReports(Long ownerId, LocalDate from, LocalDate to) {
        CurrentUser.requireSelf(ownerId);

        LocalDate rangeTo = to != null ? to : LocalDate.now();
        LocalDate rangeFrom = from != null ? from : rangeTo.minusMonths(5).withDayOfMonth(1);

        List<WorkRecord> works = workRepository.findByOwnerIdOrderByWorkIdDesc(ownerId).stream()
                .filter(w -> w.getWorkDate() != null && !w.getWorkDate().isBefore(rangeFrom) && !w.getWorkDate().isAfter(rangeTo))
                .toList();
        List<Booking> bookings = bookingRepository.findByOwnerIdOrderByBookingIdDesc(ownerId).stream()
                .filter(b -> b.getRequestedDate() != null
                        && !b.getRequestedDate().toLocalDate().isBefore(rangeFrom)
                        && !b.getRequestedDate().toLocalDate().isAfter(rangeTo))
                .toList();
        List<Payment> payments = paymentRepository.findByOwnerIdOrderByPaymentIdDesc(ownerId).stream()
                .filter(p -> p.getPaymentDate() != null
                        && !p.getPaymentDate().toLocalDate().isBefore(rangeFrom)
                        && !p.getPaymentDate().toLocalDate().isAfter(rangeTo))
                .toList();
        List<MaintenanceLog> maintenanceLogs = maintenanceLogRepository.findByOwnerIdOrderByMaintenanceIdDesc(ownerId).stream()
                .filter(m -> m.getMaintenanceDate() != null && !m.getMaintenanceDate().isBefore(rangeFrom) && !m.getMaintenanceDate().isAfter(rangeTo))
                .toList();
        List<Driver> drivers = driverRepository.findByOwnerId(ownerId);
        List<Tractor> tractors = tractorRepository.findByOwnerId(ownerId);
        List<Customer> customers = customerRepository.findByOwnerId(ownerId);
        Map<Long, String> customerNames = new HashMap<>();
        for (Customer c : customers) customerNames.put(c.getCustomerId(), c.getName());

        // --- Driver performance ---
        Map<Long, long[]> driverJobsMinutes = new HashMap<>(); // [completedJobs, totalMinutes]
        Map<Long, BigDecimal> driverRevenue = new HashMap<>();
        for (WorkRecord w : works) {
            if (w.getDriverId() == null || !"COMPLETED".equalsIgnoreCase(w.getStatus())) continue;
            long[] agg = driverJobsMinutes.computeIfAbsent(w.getDriverId(), k -> new long[2]);
            agg[0]++;
            agg[1] += w.getDurationMinutes() == null ? 0 : w.getDurationMinutes();
            if (w.getAmount() != null) {
                driverRevenue.merge(w.getDriverId(), w.getAmount(), BigDecimal::add);
            }
        }
        List<AdvancedReportsResponse.DriverPerformance> driverPerformance = new ArrayList<>();
        for (Driver d : drivers) {
            long[] agg = driverJobsMinutes.getOrDefault(d.getDriverId(), new long[2]);
            driverPerformance.add(new AdvancedReportsResponse.DriverPerformance(
                    d.getDriverId(), d.getLicenseNumber(), agg[0], agg[1],
                    driverRevenue.getOrDefault(d.getDriverId(), BigDecimal.ZERO)));
        }
        driverPerformance.sort((a, b) -> Long.compare(b.getCompletedJobs(), a.getCompletedJobs()));

        // --- Tractor utilization ---
        Map<Long, long[]> tractorJobsMinutes = new HashMap<>();
        Map<Long, BigDecimal> tractorRevenue = new HashMap<>();
        for (WorkRecord w : works) {
            if (w.getTractorId() == null || !"COMPLETED".equalsIgnoreCase(w.getStatus())) continue;
            long[] agg = tractorJobsMinutes.computeIfAbsent(w.getTractorId(), k -> new long[2]);
            agg[0]++;
            agg[1] += w.getDurationMinutes() == null ? 0 : w.getDurationMinutes();
            if (w.getAmount() != null) {
                tractorRevenue.merge(w.getTractorId(), w.getAmount(), BigDecimal::add);
            }
        }
        List<AdvancedReportsResponse.TractorUtilization> tractorUtilizationList = new ArrayList<>();
        for (Tractor t : tractors) {
            long[] agg = tractorJobsMinutes.getOrDefault(t.getTractorId(), new long[2]);
            tractorUtilizationList.add(new AdvancedReportsResponse.TractorUtilization(
                    t.getTractorId(), t.getModel(), t.getRegistrationNumber(), agg[0], agg[1],
                    tractorRevenue.getOrDefault(t.getTractorId(), BigDecimal.ZERO)));
        }
        tractorUtilizationList.sort((a, b) -> Long.compare(b.getTotalMinutesUsed(), a.getTotalMinutesUsed()));

        // --- Booking analytics ---
        Map<String, Long> byStatus = new HashMap<>();
        Map<String, Long> byMonth = new TreeMap<>();
        Map<Integer, Long> byHour = new TreeMap<>();
        DateTimeFormatter monthKeyFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        for (Booking b : bookings) {
            if (b.getStatus() != null) byStatus.merge(b.getStatus(), 1L, Long::sum);
            if (b.getRequestedDate() != null) {
                byMonth.merge(b.getRequestedDate().format(monthKeyFmt), 1L, Long::sum);
                byHour.merge(b.getRequestedDate().getHour(), 1L, Long::sum);
            }
        }
        AdvancedReportsResponse.BookingAnalytics bookingAnalytics =
                new AdvancedReportsResponse.BookingAnalytics(bookings.size(), byStatus, byMonth, byHour);

        // --- Top customers by spend ---
        Map<Long, Long> customerBookingCount = new HashMap<>();
        for (Booking b : bookings) {
            if (b.getClientId() != null) customerBookingCount.merge(b.getClientId(), 1L, Long::sum);
        }
        Map<Long, BigDecimal> customerSpend = new HashMap<>();
        for (Payment p : payments) {
            if ("SUCCESS".equalsIgnoreCase(p.getPaymentStatus()) && p.getCustomerId() != null && p.getAmount() != null) {
                customerSpend.merge(p.getCustomerId(), p.getAmount(), BigDecimal::add);
            }
        }
        List<AdvancedReportsResponse.TopCustomer> topCustomers = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : customerSpend.entrySet()) {
            topCustomers.add(new AdvancedReportsResponse.TopCustomer(
                    entry.getKey(), customerNames.getOrDefault(entry.getKey(), "Customer #" + entry.getKey()),
                    customerBookingCount.getOrDefault(entry.getKey(), 0L), entry.getValue()));
        }
        topCustomers.sort((a, b) -> b.getTotalSpent().compareTo(a.getTotalSpent()));
        if (topCustomers.size() > 10) topCustomers = topCustomers.subList(0, 10);

        // --- Payment analytics ---
        Map<String, Long> countByStatus = new HashMap<>();
        Map<String, BigDecimal> amountByMethod = new HashMap<>();
        long successCount = 0;
        for (Payment p : payments) {
            if (p.getPaymentStatus() != null) {
                countByStatus.merge(p.getPaymentStatus(), 1L, Long::sum);
                if ("SUCCESS".equalsIgnoreCase(p.getPaymentStatus())) successCount++;
            }
            if (p.getPaymentMethod() != null && p.getAmount() != null) {
                amountByMethod.merge(p.getPaymentMethod(), p.getAmount(), BigDecimal::add);
            }
        }
        double successRate = payments.isEmpty() ? 0.0 : round1((successCount * 100.0) / payments.size());
        AdvancedReportsResponse.PaymentAnalytics paymentAnalytics =
                new AdvancedReportsResponse.PaymentAnalytics(countByStatus, amountByMethod, successRate);

        // --- Maintenance cost report ---
        Map<Long, long[]> maintCount = new HashMap<>();
        Map<Long, BigDecimal> maintCost = new HashMap<>();
        for (MaintenanceLog m : maintenanceLogs) {
            maintCount.computeIfAbsent(m.getTractorId(), k -> new long[1])[0]++;
            if (m.getCost() != null) maintCost.merge(m.getTractorId(), m.getCost(), BigDecimal::add);
        }
        List<AdvancedReportsResponse.MaintenanceCost> maintenanceCosts = new ArrayList<>();
        for (Tractor t : tractors) {
            long count = maintCount.containsKey(t.getTractorId()) ? maintCount.get(t.getTractorId())[0] : 0;
            if (count == 0 && !maintCost.containsKey(t.getTractorId())) continue;
            maintenanceCosts.add(new AdvancedReportsResponse.MaintenanceCost(
                    t.getTractorId(), t.getModel(), count, maintCost.getOrDefault(t.getTractorId(), BigDecimal.ZERO)));
        }
        maintenanceCosts.sort((a, b) -> b.getTotalCost().compareTo(a.getTotalCost()));

        return new AdvancedReportsResponse(ownerId, rangeFrom.toString(), rangeTo.toString(),
                driverPerformance, tractorUtilizationList, bookingAnalytics, topCustomers, paymentAnalytics, maintenanceCosts);
    }

    // ✅ Module 5 — Advanced Analytics. Trend-series composition over the same
    // repositories used by fleetDashboard()/advancedReports(); no new tables,
    // no duplicated top-customer/driver-performance logic (that stays in
    // AdvancedReportsResponse — the frontend combines both calls).
    public AnalyticsResponse analytics(Long ownerId) {
        CurrentUser.requireSelf(ownerId);

        List<WorkRecord> works = workRepository.findByOwnerIdOrderByWorkIdDesc(ownerId);
        List<Payment> payments = paymentRepository.findByOwnerIdOrderByPaymentIdDesc(ownerId);
        List<Booking> bookings = bookingRepository.findByOwnerIdOrderByBookingIdDesc(ownerId);
        List<Customer> customers = customerRepository.findByOwnerId(ownerId);
        List<Driver> drivers = driverRepository.findByOwnerId(ownerId);

        DateTimeFormatter monthKeyFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        YearMonth currentMonth = YearMonth.from(LocalDate.now());
        Map<String, BigDecimal> revenueByMonth = new TreeMap<>();
        Map<String, BigDecimal> bookingsByMonth = new TreeMap<>();
        Map<String, BigDecimal> customersByMonth = new TreeMap<>();
        Map<String, BigDecimal> usageHoursByMonth = new TreeMap<>();
        YearMonth cursor = currentMonth.minusMonths(TREND_MONTHS - 1);
        for (int i = 0; i < TREND_MONTHS; i++) {
            String key = cursor.format(monthKeyFmt);
            revenueByMonth.put(key, BigDecimal.ZERO);
            bookingsByMonth.put(key, BigDecimal.ZERO);
            customersByMonth.put(key, BigDecimal.ZERO);
            usageHoursByMonth.put(key, BigDecimal.ZERO);
            cursor = cursor.plusMonths(1);
        }

        for (Payment p : payments) {
            if (!"SUCCESS".equalsIgnoreCase(p.getPaymentStatus()) || p.getAmount() == null || p.getPaymentDate() == null) continue;
            String key = p.getPaymentDate().format(monthKeyFmt);
            if (revenueByMonth.containsKey(key)) revenueByMonth.merge(key, p.getAmount(), BigDecimal::add);
        }
        for (Booking b : bookings) {
            if (b.getRequestedDate() == null) continue;
            String key = b.getRequestedDate().format(monthKeyFmt);
            if (bookingsByMonth.containsKey(key)) bookingsByMonth.merge(key, BigDecimal.ONE, BigDecimal::add);
        }
        for (Customer c : customers) {
            if (c.getCreatedAt() == null) continue;
            String key = c.getCreatedAt().format(monthKeyFmt);
            if (customersByMonth.containsKey(key)) customersByMonth.merge(key, BigDecimal.ONE, BigDecimal::add);
        }
        for (WorkRecord w : works) {
            if (w.getWorkDate() == null || w.getDurationMinutes() == null) continue;
            String key = w.getWorkDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            if (usageHoursByMonth.containsKey(key)) {
                BigDecimal hours = BigDecimal.valueOf(w.getDurationMinutes()).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
                usageHoursByMonth.merge(key, hours, BigDecimal::add);
            }
        }

        List<AnalyticsResponse.TrendPoint> revenueTrend = toTrendPoints(revenueByMonth);
        List<AnalyticsResponse.TrendPoint> bookingTrend = toTrendPoints(bookingsByMonth);
        List<AnalyticsResponse.TrendPoint> customerGrowthTrend = toTrendPoints(customersByMonth);
        List<AnalyticsResponse.TrendPoint> tractorUsageTrend = toTrendPoints(usageHoursByMonth);

        // --- Driver productivity (all-time, jobs per hour worked) ---
        Map<Long, long[]> driverJobsMinutes = new HashMap<>(); // [completedJobs, totalMinutes]
        for (WorkRecord w : works) {
            if (w.getDriverId() == null || !"COMPLETED".equalsIgnoreCase(w.getStatus())) continue;
            long[] agg = driverJobsMinutes.computeIfAbsent(w.getDriverId(), k -> new long[2]);
            agg[0]++;
            agg[1] += w.getDurationMinutes() == null ? 0 : w.getDurationMinutes();
        }
        List<AnalyticsResponse.DriverProductivityPoint> driverProductivity = new ArrayList<>();
        for (Driver d : drivers) {
            long[] agg = driverJobsMinutes.getOrDefault(d.getDriverId(), new long[2]);
            double hours = agg[1] / 60.0;
            double jobsPerHour = hours == 0 ? 0.0 : round1(agg[0] / hours);
            driverProductivity.add(new AnalyticsResponse.DriverProductivityPoint(
                    d.getDriverId(), d.getLicenseNumber(), agg[0], jobsPerHour));
        }
        driverProductivity.sort((a, b) -> Double.compare(b.getJobsPerHour(), a.getJobsPerHour()));

        // --- Seasonal booking pattern (month-of-year, across all years) ---
        Map<String, Long> seasonalPattern = new TreeMap<>();
        for (int m = 1; m <= 12; m++) seasonalPattern.put(String.format("%02d", m), 0L);
        Map<Integer, Long> peakHours = new TreeMap<>();
        for (Booking b : bookings) {
            if (b.getRequestedDate() == null) continue;
            String monthOfYear = String.format("%02d", b.getRequestedDate().getMonthValue());
            seasonalPattern.merge(monthOfYear, 1L, Long::sum);
            peakHours.merge(b.getRequestedDate().getHour(), 1L, Long::sum);
        }

        AnalyticsResponse response = new AnalyticsResponse();
        response.setOwnerId(ownerId);
        response.setRevenueTrend(revenueTrend);
        response.setBookingTrend(bookingTrend);
        response.setCustomerGrowthTrend(customerGrowthTrend);
        response.setTractorUsageTrend(tractorUsageTrend);
        response.setDriverProductivity(driverProductivity);
        response.setSeasonalBookingPattern(seasonalPattern);
        response.setPeakBookingHours(peakHours);
        return response;
    }

    private List<AnalyticsResponse.TrendPoint> toTrendPoints(Map<String, BigDecimal> source) {
        List<AnalyticsResponse.TrendPoint> points = new ArrayList<>();
        source.forEach((k, v) -> points.add(new AnalyticsResponse.TrendPoint(k, v)));
        return points;
    }

    // ✅ Module 7 — AI Features. Statistical forecast + rule-based insights,
    // composed from analytics()/fleetDashboard()-style aggregation over
    // existing repositories. No ML infra, no new persisted state.
    public AiInsightsResponse insights(Long ownerId) {
        CurrentUser.requireSelf(ownerId);

        AnalyticsResponse analyticsData = analytics(ownerId);
        List<Tractor> tractors = tractorRepository.findByOwnerId(ownerId);
        List<Booking> bookings = bookingRepository.findByOwnerIdOrderByBookingIdDesc(ownerId);
        List<WorkRecord> works = workRepository.findByOwnerIdOrderByWorkIdDesc(ownerId);
        LocalDate today = LocalDate.now();

        // --- Revenue forecast: simple linear regression over the 12-month trend ---
        List<AnalyticsResponse.TrendPoint> trend = analyticsData.getRevenueTrend();
        List<AiInsightsResponse.RevenueForecastPoint> forecast = forecastRevenue(trend);

        // --- Rule-based insights ---
        List<AiInsightsResponse.Insight> insights = new ArrayList<>();

        if (trend.size() >= 2) {
            BigDecimal last = trend.get(trend.size() - 1).getValue();
            BigDecimal prev = trend.get(trend.size() - 2).getValue();
            if (prev.compareTo(BigDecimal.ZERO) > 0) {
                double changePercent = round1(last.subtract(prev).divide(prev, 4, RoundingMode.HALF_UP).doubleValue() * 100);
                if (changePercent >= 10) {
                    insights.add(new AiInsightsResponse.Insight("REVENUE", "SUCCESS", "Revenue is trending up",
                            String.format("This month's revenue is up %.1f%% versus last month.", changePercent)));
                } else if (changePercent <= -10) {
                    insights.add(new AiInsightsResponse.Insight("REVENUE", "WARNING", "Revenue is trending down",
                            String.format("This month's revenue is down %.1f%% versus last month — check for idle tractors or lost bookings.", Math.abs(changePercent))));
                }
            }
        }

        long idleTractors = tractors.stream().filter(t -> "AVAILABLE".equalsIgnoreCase(t.getStatus())).count();
        if (tractors.size() > 0 && idleTractors == tractors.size()) {
            insights.add(new AiInsightsResponse.Insight("BOOKING", "WARNING", "Entire fleet is idle",
                    "None of your tractors have an active job right now — consider promoting available slots to customers."));
        }

        long pendingBookings = bookings.stream().filter(b -> "PENDING".equalsIgnoreCase(b.getStatus())).count();
        if (pendingBookings >= 3) {
            insights.add(new AiInsightsResponse.Insight("BOOKING", "WARNING", "Booking requests awaiting response",
                    pendingBookings + " booking requests are still pending — faster responses improve conversion."));
        }

        List<TractorDocument> expiringSoon = tractorDocumentRepository
                .findByOwnerIdAndExpiryDateBetweenAndReminderSentFalse(ownerId, today, today.plusDays(30));
        if (!expiringSoon.isEmpty()) {
            insights.add(new AiInsightsResponse.Insight("DOCUMENT", "WARNING", "Documents expiring soon",
                    expiringSoon.size() + " tractor document(s) expire within 30 days — renew them to avoid compliance issues."));
        }

        if (!analyticsData.getDriverProductivity().isEmpty()) {
            AnalyticsResponse.DriverProductivityPoint topDriver = analyticsData.getDriverProductivity().get(0);
            if (topDriver.getCompletedJobs() > 0) {
                insights.add(new AiInsightsResponse.Insight("DRIVER", "INFO", "Top performing driver",
                        (topDriver.getLicenseNumber() != null ? topDriver.getLicenseNumber() : "Driver #" + topDriver.getDriverId())
                                + " is your most productive driver at " + topDriver.getJobsPerHour() + " jobs/hour."));
            }
        }

        if (insights.isEmpty()) {
            insights.add(new AiInsightsResponse.Insight("GENERAL", "INFO", "All steady",
                    "No unusual patterns detected in your business data right now."));
        }

        // --- Tractor recommendations for the next booking: prefer available,
        // under-utilized (idle longer), higher-earning tractors ---
        Map<Long, LocalDate> lastJobDate = new HashMap<>();
        Map<Long, BigDecimal> tractorRevenue90d = new HashMap<>();
        LocalDate ninetyDaysAgo = today.minusDays(90);
        for (WorkRecord w : works) {
            if (w.getTractorId() == null || w.getWorkDate() == null) continue;
            lastJobDate.merge(w.getTractorId(), w.getWorkDate(), (a, b) -> a.isAfter(b) ? a : b);
            if (!w.getWorkDate().isBefore(ninetyDaysAgo) && w.getAmount() != null) {
                tractorRevenue90d.merge(w.getTractorId(), w.getAmount(), BigDecimal::add);
            }
        }
        List<AiInsightsResponse.TractorRecommendation> recommendations = new ArrayList<>();
        for (Tractor t : tractors) {
            if (!"AVAILABLE".equalsIgnoreCase(t.getStatus())) continue;
            LocalDate last = lastJobDate.get(t.getTractorId());
            long idleDays = last == null ? 999 : ChronoUnit.DAYS.between(last, today);
            double revenueScore = tractorRevenue90d.getOrDefault(t.getTractorId(), BigDecimal.ZERO).doubleValue();
            double score = round1(Math.min(idleDays, 60) * 0.5 + revenueScore * 0.01);
            String reason = last == null
                    ? "Never assigned a job yet — a good candidate to bring into rotation."
                    : idleDays >= 14
                        ? "Idle for " + idleDays + " days and has strong recent earnings."
                        : "Available now and performing well.";
            recommendations.add(new AiInsightsResponse.TractorRecommendation(
                    t.getTractorId(), t.getModel(), t.getRegistrationNumber(), reason, score));
        }
        recommendations.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        if (recommendations.size() > 5) recommendations = recommendations.subList(0, 5);

        return new AiInsightsResponse(ownerId, forecast, insights, recommendations);
    }

    private List<AiInsightsResponse.RevenueForecastPoint> forecastRevenue(List<AnalyticsResponse.TrendPoint> trend) {
        List<AiInsightsResponse.RevenueForecastPoint> forecast = new ArrayList<>();
        if (trend == null || trend.size() < 2) return forecast;

        int n = trend.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = trend.get(i).getValue().doubleValue();
            sumX += x; sumY += y; sumXY += x * y; sumXX += x * x;
        }
        double denominator = (n * sumXX - sumX * sumX);
        double slope = denominator == 0 ? 0 : (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;

        String lastPeriod = trend.get(n - 1).getPeriod();
        YearMonth cursor = YearMonth.parse(lastPeriod);
        for (int i = 1; i <= FORECAST_MONTHS; i++) {
            cursor = cursor.plusMonths(1);
            double predicted = intercept + slope * (n - 1 + i);
            BigDecimal estimate = BigDecimal.valueOf(Math.max(0, predicted)).setScale(2, RoundingMode.HALF_UP);
            forecast.add(new AiInsightsResponse.RevenueForecastPoint(
                    cursor.format(DateTimeFormatter.ofPattern("yyyy-MM")), estimate));
        }
        return forecast;
    }
}
