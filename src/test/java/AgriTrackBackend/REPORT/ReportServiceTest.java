package AgriTrackBackend.REPORT;

import AgriTrackBackend.AUDIT.AuditLogRepository;
import AgriTrackBackend.BOOKING.Booking;
import AgriTrackBackend.BOOKING.BookingRepository;
import AgriTrackBackend.CUSTOMER.CustomerRepository;
import AgriTrackBackend.DRIVER.Driver;
import AgriTrackBackend.DRIVER.DriverRepository;
import AgriTrackBackend.EXCEPTION.ForbiddenException;
import AgriTrackBackend.MAINTENANCE.MaintenanceLog;
import AgriTrackBackend.MAINTENANCE.MaintenanceLogRepository;
import AgriTrackBackend.PAYMENT.Payment;
import AgriTrackBackend.PAYMENT.PaymentRepository;
import AgriTrackBackend.RATING.RatingService;
import AgriTrackBackend.SECURITY.AuthenticatedUser;
import AgriTrackBackend.TRACTOR.Tractor;
import AgriTrackBackend.TRACTOR.TractorRepository;
import AgriTrackBackend.WORK.WorkRecord;
import AgriTrackBackend.WORK.WorkRecordRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Fleet Dashboard is a pure read/aggregate composition over data already
 * owned by other repositories — this verifies the aggregation math (revenue
 * buckets, utilization percentages, maintenance-due heuristic, chart
 * bucketing) and that it's still ownership-gated like every other report.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private WorkRecordRepository workRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private TractorRepository tractorRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private MaintenanceLogRepository maintenanceLogRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private RatingService ratingService;

    @InjectMocks
    private ReportService service;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(Long userId) {
        AuthenticatedUser principal = new AuthenticatedUser(userId, "owner@example.com", "OWNER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @Test
    void fleetDashboardRejectsRequestForAnotherOwnersData() {
        loginAs(1L);
        assertThatThrownBy(() -> service.fleetDashboard(2L)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void fleetDashboardAggregatesRevenueJobsUtilizationAndMaintenance() {
        loginAs(1L);

        // revenue: one payment today, one earlier this month, one last month (outside "today"/partly "month")
        Payment todayPayment = payment(new BigDecimal("500"), "SUCCESS", LocalDateTime.now());
        Payment monthPayment = payment(new BigDecimal("300"), "SUCCESS",
                LocalDate.now().withDayOfMonth(1).atStartOfDay().plusDays(1));
        Payment pendingPayment = payment(new BigDecimal("200"), "PENDING", LocalDateTime.now());
        when(paymentRepository.findByOwnerIdOrderByPaymentIdDesc(1L))
                .thenReturn(List.of(todayPayment, monthPayment, pendingPayment));

        // jobs
        WorkRecord running = new WorkRecord();
        running.setStatus("RUNNING");
        WorkRecord completed = new WorkRecord();
        completed.setStatus("COMPLETED");
        when(workRepository.findByOwnerIdOrderByWorkIdDesc(1L)).thenReturn(List.of(running, completed));

        // bookings
        Booking todayBooking = new Booking();
        todayBooking.setRequestedDate(LocalDateTime.now());
        todayBooking.setStatus("PENDING");
        Booking futureBooking = new Booking();
        futureBooking.setRequestedDate(LocalDateTime.now().plusDays(3));
        futureBooking.setStatus("ACCEPTED");
        when(bookingRepository.findByOwnerIdOrderByBookingIdDesc(1L)).thenReturn(List.of(todayBooking, futureBooking));

        // fleet utilization: 1 of 2 tractors busy, 1 of 2 drivers busy
        Tractor busyTractor = new Tractor();
        busyTractor.setTractorId(10L);
        busyTractor.setStatus("BUSY");
        busyTractor.setModel("Mahindra 575");
        busyTractor.setRegistrationNumber("TN-01-AB-1234");
        Tractor idleTractor = new Tractor();
        idleTractor.setTractorId(11L);
        idleTractor.setStatus("AVAILABLE");
        idleTractor.setModel("Swaraj 744");
        when(tractorRepository.findByOwnerId(1L)).thenReturn(List.of(busyTractor, idleTractor));

        Driver busyDriver = new Driver();
        busyDriver.setIsAvailable(false);
        Driver freeDriver = new Driver();
        freeDriver.setIsAvailable(true);
        when(driverRepository.findByOwnerId(1L)).thenReturn(List.of(busyDriver, freeDriver));

        // maintenance: idleTractor never serviced -> due; busyTractor serviced yesterday -> not due
        MaintenanceLog recentLog = new MaintenanceLog();
        recentLog.setTractorId(10L);
        recentLog.setMaintenanceDate(LocalDate.now().minusDays(1));
        when(maintenanceLogRepository.findByOwnerIdOrderByMaintenanceIdDesc(1L)).thenReturn(List.of(recentLog));

        when(auditLogRepository.search(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        FleetDashboardResponse dashboard = service.fleetDashboard(1L);

        assertThat(dashboard.getTodayRevenue()).isEqualByComparingTo("500");
        assertThat(dashboard.getMonthRevenue()).isEqualByComparingTo("800");
        assertThat(dashboard.getTotalRevenue()).isEqualByComparingTo("800");
        assertThat(dashboard.getPendingPaymentsAmount()).isEqualByComparingTo("200");
        assertThat(dashboard.getPendingPaymentsCount()).isEqualTo(1);

        assertThat(dashboard.getActiveJobsCount()).isEqualTo(1);
        assertThat(dashboard.getTodaysBookingsCount()).isEqualTo(1);
        assertThat(dashboard.getUpcomingJobsCount()).isEqualTo(1);

        assertThat(dashboard.getTotalTractors()).isEqualTo(2);
        assertThat(dashboard.getBusyTractors()).isEqualTo(1);
        assertThat(dashboard.getTractorUtilizationPercent()).isEqualTo(50.0);

        assertThat(dashboard.getTotalDrivers()).isEqualTo(2);
        assertThat(dashboard.getBusyDrivers()).isEqualTo(1);
        assertThat(dashboard.getDriverUtilizationPercent()).isEqualTo(50.0);

        assertThat(dashboard.getMonthlyRevenueChart()).hasSize(6);

        assertThat(dashboard.getUpcomingMaintenance())
                .extracting(FleetDashboardResponse.UpcomingMaintenanceItem::getTractorId)
                .containsExactly(11L); // never-serviced tractor is due; recently-serviced one is not
    }

    @Test
    void fleetDashboardHandlesEmptyFleetWithoutDivideByZero() {
        loginAs(1L);
        when(paymentRepository.findByOwnerIdOrderByPaymentIdDesc(1L)).thenReturn(List.of());
        when(workRepository.findByOwnerIdOrderByWorkIdDesc(1L)).thenReturn(List.of());
        when(bookingRepository.findByOwnerIdOrderByBookingIdDesc(1L)).thenReturn(List.of());
        when(tractorRepository.findByOwnerId(1L)).thenReturn(List.of());
        when(driverRepository.findByOwnerId(1L)).thenReturn(List.of());
        when(maintenanceLogRepository.findByOwnerIdOrderByMaintenanceIdDesc(1L)).thenReturn(List.of());
        when(auditLogRepository.search(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        FleetDashboardResponse dashboard = service.fleetDashboard(1L);

        assertThat(dashboard.getTractorUtilizationPercent()).isEqualTo(0.0);
        assertThat(dashboard.getDriverUtilizationPercent()).isEqualTo(0.0);
        assertThat(dashboard.getTotalRevenue()).isEqualByComparingTo("0");
        assertThat(dashboard.getUpcomingMaintenance()).isEmpty();
    }

    private Payment payment(BigDecimal amount, String status, LocalDateTime date) {
        Payment p = new Payment();
        p.setAmount(amount);
        p.setPaymentStatus(status);
        p.setPaymentDate(date);
        return p;
    }
}
