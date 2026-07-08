package AgriTrackBackend.REPORT;

import AgriTrackBackend.AUDIT.AuditLogRepository;
import AgriTrackBackend.BOOKING.Booking;
import AgriTrackBackend.BOOKING.BookingRepository;
import AgriTrackBackend.CUSTOMER.Customer;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Module 4 — verifies the advanced-reports aggregation (driver performance,
 * tractor utilization, booking/customer/payment analytics, maintenance
 * cost) composes correctly from existing repository data.
 */
@ExtendWith(MockitoExtension.class)
class AdvancedReportsTest {

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
    void advancedReportsRejectsRequestForAnotherOwner() {
        loginAs(1L);
        assertThatThrownBy(() -> service.advancedReports(2L, null, null)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void advancedReportsAggregatesAcrossAllCategories() {
        loginAs(1L);
        LocalDate today = LocalDate.now();

        Driver driver = new Driver();
        driver.setDriverId(10L);
        driver.setLicenseNumber("DL123");
        when(driverRepository.findByOwnerId(1L)).thenReturn(List.of(driver));

        Tractor tractor = new Tractor();
        tractor.setTractorId(20L);
        tractor.setModel("Mahindra 575");
        tractor.setRegistrationNumber("TN-01-AB-1234");
        when(tractorRepository.findByOwnerId(1L)).thenReturn(List.of(tractor));

        Customer customer = new Customer();
        customer.setCustomerId(30L);
        customer.setName("Ravi Farms");
        when(customerRepository.findByOwnerId(1L)).thenReturn(List.of(customer));

        WorkRecord completed = new WorkRecord();
        completed.setDriverId(10L);
        completed.setTractorId(20L);
        completed.setStatus("COMPLETED");
        completed.setWorkDate(today);
        completed.setDurationMinutes(120L);
        completed.setAmount(new BigDecimal("1500"));
        when(workRepository.findByOwnerIdOrderByWorkIdDesc(1L)).thenReturn(List.of(completed));

        Booking booking = new Booking();
        booking.setClientId(30L);
        booking.setStatus("COMPLETED");
        booking.setRequestedDate(today.atTime(14, 0));
        when(bookingRepository.findByOwnerIdOrderByBookingIdDesc(1L)).thenReturn(List.of(booking));

        Payment payment = new Payment();
        payment.setCustomerId(30L);
        payment.setPaymentStatus("SUCCESS");
        payment.setPaymentMethod("UPI");
        payment.setAmount(new BigDecimal("1500"));
        payment.setPaymentDate(today.atTime(15, 0));
        when(paymentRepository.findByOwnerIdOrderByPaymentIdDesc(1L)).thenReturn(List.of(payment));

        MaintenanceLog log = new MaintenanceLog();
        log.setTractorId(20L);
        log.setCost(new BigDecimal("500"));
        log.setMaintenanceDate(today);
        when(maintenanceLogRepository.findByOwnerIdOrderByMaintenanceIdDesc(1L)).thenReturn(List.of(log));

        AdvancedReportsResponse result = service.advancedReports(1L, today.minusDays(5), today.plusDays(1));

        assertThat(result.getDriverPerformance()).hasSize(1);
        assertThat(result.getDriverPerformance().get(0).getCompletedJobs()).isEqualTo(1);
        assertThat(result.getDriverPerformance().get(0).getRevenueGenerated()).isEqualByComparingTo("1500");

        assertThat(result.getTractorUtilization()).hasSize(1);
        assertThat(result.getTractorUtilization().get(0).getTotalMinutesUsed()).isEqualTo(120);

        assertThat(result.getBookingAnalytics().getTotalBookings()).isEqualTo(1);
        assertThat(result.getBookingAnalytics().getByStatus().get("COMPLETED")).isEqualTo(1L);
        assertThat(result.getBookingAnalytics().getByHourOfDay().get(14)).isEqualTo(1L);

        assertThat(result.getTopCustomers()).hasSize(1);
        assertThat(result.getTopCustomers().get(0).getName()).isEqualTo("Ravi Farms");
        assertThat(result.getTopCustomers().get(0).getTotalSpent()).isEqualByComparingTo("1500");

        assertThat(result.getPaymentAnalytics().getCountByStatus().get("SUCCESS")).isEqualTo(1L);
        assertThat(result.getPaymentAnalytics().getAmountByMethod().get("UPI")).isEqualByComparingTo("1500");
        assertThat(result.getPaymentAnalytics().getSuccessRatePercent()).isEqualTo(100.0);

        assertThat(result.getMaintenanceCosts()).hasSize(1);
        assertThat(result.getMaintenanceCosts().get(0).getTotalCost()).isEqualByComparingTo("500");
    }

    @Test
    void advancedReportsExcludesRecordsOutsideTheDateRange() {
        loginAs(1L);
        LocalDate today = LocalDate.now();

        when(driverRepository.findByOwnerId(1L)).thenReturn(List.of());
        when(tractorRepository.findByOwnerId(1L)).thenReturn(List.of());
        when(customerRepository.findByOwnerId(1L)).thenReturn(List.of());

        Booking outOfRange = new Booking();
        outOfRange.setStatus("COMPLETED");
        outOfRange.setRequestedDate(today.minusMonths(3).atTime(10, 0));
        when(bookingRepository.findByOwnerIdOrderByBookingIdDesc(1L)).thenReturn(List.of(outOfRange));

        when(workRepository.findByOwnerIdOrderByWorkIdDesc(1L)).thenReturn(List.of());
        when(paymentRepository.findByOwnerIdOrderByPaymentIdDesc(1L)).thenReturn(List.of());
        when(maintenanceLogRepository.findByOwnerIdOrderByMaintenanceIdDesc(1L)).thenReturn(List.of());

        AdvancedReportsResponse result = service.advancedReports(1L, today.minusDays(5), today);

        assertThat(result.getBookingAnalytics().getTotalBookings()).isEqualTo(0);
    }
}
