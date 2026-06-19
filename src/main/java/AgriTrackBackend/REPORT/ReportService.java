package AgriTrackBackend.REPORT;

import AgriTrackBackend.BOOKING.Booking;
import AgriTrackBackend.BOOKING.BookingRepository;
import AgriTrackBackend.CUSTOMER.CustomerRepository;
import AgriTrackBackend.PAYMENT.Payment;
import AgriTrackBackend.PAYMENT.PaymentRepository;
import AgriTrackBackend.RATING.RatingService;
import AgriTrackBackend.WORK.WorkRecord;
import AgriTrackBackend.WORK.WorkRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private RatingService ratingService;

    // ✅ Owner dashboard summary
    public Map<String, Object> ownerSummary(Long ownerId) {
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
}
