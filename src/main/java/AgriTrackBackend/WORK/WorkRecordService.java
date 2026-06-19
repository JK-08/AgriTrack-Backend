package AgriTrackBackend.WORK;

import AgriTrackBackend.RATE.Rate;
import AgriTrackBackend.RATE.RateRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WorkRecordService {

    @Autowired
    private WorkRecordRepository repository;

    @Autowired
    private RateRepository rateRepository;

    // ✅ START a timer-based work session
    @Transactional
    public WorkRecord start(StartWorkRequest req) {
        WorkRecord w = new WorkRecord();
        w.setOwnerId(req.getOwnerId());
        w.setCustomerId(req.getCustomerId());
        w.setTractorId(req.getTractorId());
        w.setRateId(req.getRateId());
        w.setServiceType(req.getServiceType());
        w.setNotes(req.getNotes());
        w.setWorkDate(LocalDate.now());
        LocalDateTime now = LocalDateTime.now();
        w.setStartTime(now);
        w.setLastResumeTime(now);
        w.setAccumulatedSeconds(0L);
        w.setStatus("RUNNING");
        return repository.saveAndFlush(w);
    }

    // ✅ PAUSE — bank elapsed seconds since last resume
    @Transactional
    public WorkRecord pause(Long id) {
        WorkRecord w = getById(id);
        if ("RUNNING".equals(w.getStatus()) && w.getLastResumeTime() != null) {
            long elapsed = Duration.between(w.getLastResumeTime(), LocalDateTime.now()).getSeconds();
            w.setAccumulatedSeconds(safe(w.getAccumulatedSeconds()) + elapsed);
            w.setLastResumeTime(null);
            w.setStatus("PAUSED");
        }
        return repository.saveAndFlush(w);
    }

    // ✅ RESUME
    @Transactional
    public WorkRecord resume(Long id) {
        WorkRecord w = getById(id);
        if ("PAUSED".equals(w.getStatus())) {
            w.setLastResumeTime(LocalDateTime.now());
            w.setStatus("RUNNING");
        }
        return repository.saveAndFlush(w);
    }

    // ✅ STOP / COMPLETE — finalize duration & amount
    @Transactional
    public WorkRecord stop(Long id, BigDecimal extraCharges) {
        WorkRecord w = getById(id);
        LocalDateTime now = LocalDateTime.now();

        long total = safe(w.getAccumulatedSeconds());
        if ("RUNNING".equals(w.getStatus()) && w.getLastResumeTime() != null) {
            total += Duration.between(w.getLastResumeTime(), now).getSeconds();
        }
        w.setAccumulatedSeconds(total);
        w.setEndTime(now);
        w.setLastResumeTime(null);

        long minutes = Math.max(0, Math.round(total / 60.0));
        w.setDurationMinutes(minutes);

        BigDecimal amount = calculateAmount(w.getRateId(), total);
        if (extraCharges != null) {
            w.setExtraCharges(extraCharges);
            amount = amount.add(extraCharges);
        }
        w.setAmount(amount);
        w.setStatus("COMPLETED");
        return repository.saveAndFlush(w);
    }

    // ✅ Manual entry (no live timer) — owner enters duration directly
    @Transactional
    public WorkRecord saveManual(WorkRecord w) {
        if (w.getWorkDate() == null) w.setWorkDate(LocalDate.now());
        long seconds = w.getDurationMinutes() != null ? w.getDurationMinutes() * 60 : 0;
        w.setAccumulatedSeconds(seconds);
        BigDecimal amount = calculateAmount(w.getRateId(), seconds);
        if (w.getExtraCharges() != null) {
            amount = amount.add(w.getExtraCharges());
        }
        w.setAmount(amount);
        if (w.getStatus() == null) w.setStatus("COMPLETED");
        return repository.saveAndFlush(w);
    }

    private BigDecimal calculateAmount(Long rateId, long totalSeconds) {
        if (rateId == null) return BigDecimal.ZERO;
        Rate rate = rateRepository.findById(rateId).orElse(null);
        if (rate == null) return BigDecimal.ZERO;

        BigDecimal minutes = BigDecimal.valueOf(totalSeconds)
                .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);

        if (rate.getPricePerMinute() != null && rate.getPricePerMinute().signum() > 0) {
            return rate.getPricePerMinute().multiply(minutes).setScale(2, RoundingMode.HALF_UP);
        }
        if (rate.getPricePerHour() != null && rate.getPricePerHour().signum() > 0) {
            BigDecimal hours = minutes.divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
            return rate.getPricePerHour().multiply(hours).setScale(2, RoundingMode.HALF_UP);
        }
        if (rate.getPricePerTenMinutes() != null && rate.getPricePerTenMinutes().signum() > 0) {
            BigDecimal tens = minutes.divide(BigDecimal.valueOf(10), 4, RoundingMode.HALF_UP);
            return rate.getPricePerTenMinutes().multiply(tens).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private long safe(Long v) {
        return v == null ? 0L : v;
    }

    public List<WorkRecord> getAll() {
        return repository.findAll();
    }

    public List<WorkRecord> getByOwner(Long ownerId) {
        return repository.findByOwnerIdOrderByWorkIdDesc(ownerId);
    }

    public List<WorkRecord> getByCustomer(Long customerId) {
        return repository.findByCustomerIdOrderByWorkIdDesc(customerId);
    }

    public WorkRecord getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Work record not found with id: " + id));
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
