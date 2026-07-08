package AgriTrackBackend.REPORT;

import AgriTrackBackend.AUDIT.AuditLogRepository;
import AgriTrackBackend.BOOKING.Booking;
import AgriTrackBackend.BOOKING.BookingRepository;
import AgriTrackBackend.CUSTOMER.CustomerRepository;
import AgriTrackBackend.DOCUMENT.TractorDocumentRepository;
import AgriTrackBackend.DRIVER.Driver;
import AgriTrackBackend.DRIVER.DriverRepository;
import AgriTrackBackend.EXCEPTION.ForbiddenException;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Module 7 — verifies the AI insights composition: revenue forecast (linear
 * trend), rule-based insights (idle fleet, pending bookings), and tractor
 * recommendations, all derived from existing repository data.
 */
@ExtendWith(MockitoExtension.class)
class AiInsightsTest {

    @Mock private WorkRecordRepository workRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private TractorRepository tractorRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private MaintenanceLogRepository maintenanceLogRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private RatingService ratingService;
    @Mock private TractorDocumentRepository tractorDocumentRepository;

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
    void insightsRejectsRequestForAnotherOwner() {
        loginAs(1L);
        assertThatThrownBy(() -> service.insights(2L)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void insightsFlagsIdleFleetAndPendingBookingsAndRecommendsAvailableTractor() {
        loginAs(1L);
        LocalDate today = LocalDate.now();

        Tractor tractor = new Tractor();
        tractor.setTractorId(20L);
        tractor.setModel("Mahindra 575");
        tractor.setRegistrationNumber("TN-01-AB-1234");
        tractor.setStatus("AVAILABLE");
        when(tractorRepository.findByOwnerId(1L)).thenReturn(List.of(tractor));

        when(driverRepository.findByOwnerId(1L)).thenReturn(List.of());
        when(customerRepository.findByOwnerId(1L)).thenReturn(List.of());
        when(tractorDocumentRepository.findByOwnerIdAndExpiryDateBetweenAndReminderSentFalse(
                1L, today, today.plusDays(30))).thenReturn(List.of());

        Booking pending1 = new Booking(); pending1.setStatus("PENDING");
        Booking pending2 = new Booking(); pending2.setStatus("PENDING");
        Booking pending3 = new Booking(); pending3.setStatus("PENDING");
        when(bookingRepository.findByOwnerIdOrderByBookingIdDesc(1L)).thenReturn(List.of(pending1, pending2, pending3));

        WorkRecord oldJob = new WorkRecord();
        oldJob.setTractorId(20L);
        oldJob.setWorkDate(today.minusDays(20));
        oldJob.setAmount(new BigDecimal("2000"));
        oldJob.setStatus("COMPLETED");
        when(workRepository.findByOwnerIdOrderByWorkIdDesc(1L)).thenReturn(List.of(oldJob));

        when(paymentRepository.findByOwnerIdOrderByPaymentIdDesc(1L)).thenReturn(List.of());
        when(maintenanceLogRepository.findByOwnerIdOrderByMaintenanceIdDesc(1L)).thenReturn(List.of());

        AiInsightsResponse result = service.insights(1L);

        assertThat(result.getInsights()).anyMatch(i -> i.getTitle().equals("Entire fleet is idle"));
        assertThat(result.getInsights()).anyMatch(i -> i.getTitle().equals("Booking requests awaiting response"));
        assertThat(result.getRecommendedTractors()).hasSize(1);
        assertThat(result.getRecommendedTractors().get(0).getTractorId()).isEqualTo(20L);
        assertThat(result.getRevenueForecast()).hasSize(3); // 12-month trend is always zero-filled, so forecast always runs
    }

    @Test
    void insightsForecastsRevenueFromLinearTrend() {
        loginAs(1L);
        LocalDate today = LocalDate.now();

        when(tractorRepository.findByOwnerId(1L)).thenReturn(List.of());
        when(driverRepository.findByOwnerId(1L)).thenReturn(List.of());
        when(customerRepository.findByOwnerId(1L)).thenReturn(List.of());
        when(bookingRepository.findByOwnerIdOrderByBookingIdDesc(1L)).thenReturn(List.of());
        when(workRepository.findByOwnerIdOrderByWorkIdDesc(1L)).thenReturn(List.of());
        when(maintenanceLogRepository.findByOwnerIdOrderByMaintenanceIdDesc(1L)).thenReturn(List.of());
        when(tractorDocumentRepository.findByOwnerIdAndExpiryDateBetweenAndReminderSentFalse(
                1L, today, today.plusDays(30))).thenReturn(List.of());

        Payment thisMonth = new Payment();
        thisMonth.setPaymentStatus("SUCCESS");
        thisMonth.setAmount(new BigDecimal("5000"));
        thisMonth.setPaymentDate(today.atTime(10, 0));
        Payment lastMonth = new Payment();
        lastMonth.setPaymentStatus("SUCCESS");
        lastMonth.setAmount(new BigDecimal("1000"));
        lastMonth.setPaymentDate(today.minusMonths(1).atTime(10, 0));
        when(paymentRepository.findByOwnerIdOrderByPaymentIdDesc(1L)).thenReturn(List.of(thisMonth, lastMonth));

        AiInsightsResponse result = service.insights(1L);

        assertThat(result.getRevenueForecast()).hasSize(3);
        assertThat(result.getInsights()).anyMatch(i -> i.getTitle().equals("Revenue is trending up"));
    }
}
