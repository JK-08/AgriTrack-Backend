package AgriTrackBackend.REPORT;

import AgriTrackBackend.AUDIT.AuditLogRepository;
import AgriTrackBackend.BOOKING.Booking;
import AgriTrackBackend.BOOKING.BookingRepository;
import AgriTrackBackend.CUSTOMER.Customer;
import AgriTrackBackend.CUSTOMER.CustomerRepository;
import AgriTrackBackend.DRIVER.Driver;
import AgriTrackBackend.DRIVER.DriverRepository;
import AgriTrackBackend.EXCEPTION.ForbiddenException;
import AgriTrackBackend.MAINTENANCE.MaintenanceLogRepository;
import AgriTrackBackend.PAYMENT.Payment;
import AgriTrackBackend.PAYMENT.PaymentRepository;
import AgriTrackBackend.RATING.RatingService;
import AgriTrackBackend.SECURITY.AuthenticatedUser;
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
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Module 5 — verifies the advanced-analytics trend aggregation (revenue/
 * booking/customer-growth/tractor-usage 12-month trends, driver productivity,
 * seasonal pattern, peak hours) composes correctly from existing repository
 * data.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsTest {

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
    void analyticsRejectsRequestForAnotherOwner() {
        loginAs(1L);
        assertThatThrownBy(() -> service.analytics(2L)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void analyticsAggregatesTrendsAndSeasonalPattern() {
        loginAs(1L);
        LocalDate today = LocalDate.now();
        String thisMonthKey = YearMonth.from(today).format(DateTimeFormatter.ofPattern("yyyy-MM"));

        Driver driver = new Driver();
        driver.setDriverId(10L);
        driver.setLicenseNumber("DL123");
        when(driverRepository.findByOwnerId(1L)).thenReturn(List.of(driver));

        Customer customer = new Customer();
        customer.setCustomerId(30L);
        customer.setCreatedAt(today.atStartOfDay());
        when(customerRepository.findByOwnerId(1L)).thenReturn(List.of(customer));

        WorkRecord completed = new WorkRecord();
        completed.setDriverId(10L);
        completed.setStatus("COMPLETED");
        completed.setWorkDate(today);
        completed.setDurationMinutes(120L);
        completed.setAmount(new BigDecimal("1500"));
        when(workRepository.findByOwnerIdOrderByWorkIdDesc(1L)).thenReturn(List.of(completed));

        Booking booking = new Booking();
        booking.setStatus("COMPLETED");
        booking.setRequestedDate(today.atTime(14, 0));
        when(bookingRepository.findByOwnerIdOrderByBookingIdDesc(1L)).thenReturn(List.of(booking));

        Payment payment = new Payment();
        payment.setPaymentStatus("SUCCESS");
        payment.setAmount(new BigDecimal("1500"));
        payment.setPaymentDate(today.atTime(15, 0));
        when(paymentRepository.findByOwnerIdOrderByPaymentIdDesc(1L)).thenReturn(List.of(payment));

        AnalyticsResponse result = service.analytics(1L);

        assertThat(result.getRevenueTrend()).hasSize(12);
        assertThat(result.getRevenueTrend().stream()
                .filter(p -> p.getPeriod().equals(thisMonthKey)).findFirst().orElseThrow().getValue())
                .isEqualByComparingTo("1500");

        assertThat(result.getBookingTrend().stream()
                .filter(p -> p.getPeriod().equals(thisMonthKey)).findFirst().orElseThrow().getValue())
                .isEqualByComparingTo("1");

        assertThat(result.getCustomerGrowthTrend().stream()
                .filter(p -> p.getPeriod().equals(thisMonthKey)).findFirst().orElseThrow().getValue())
                .isEqualByComparingTo("1");

        assertThat(result.getTractorUsageTrend().stream()
                .filter(p -> p.getPeriod().equals(thisMonthKey)).findFirst().orElseThrow().getValue())
                .isEqualByComparingTo("2.00");

        assertThat(result.getDriverProductivity()).hasSize(1);
        assertThat(result.getDriverProductivity().get(0).getCompletedJobs()).isEqualTo(1);
        assertThat(result.getDriverProductivity().get(0).getJobsPerHour()).isEqualTo(0.5);

        String monthOfYear = String.format("%02d", today.getMonthValue());
        assertThat(result.getSeasonalBookingPattern().get(monthOfYear)).isEqualTo(1L);
        assertThat(result.getSeasonalBookingPattern()).hasSize(12);
        assertThat(result.getPeakBookingHours().get(14)).isEqualTo(1L);
    }
}
