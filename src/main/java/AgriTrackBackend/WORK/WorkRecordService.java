package AgriTrackBackend.WORK;

import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.COMMON.PageResponse;
import AgriTrackBackend.COMMON.PaginationUtil;
import AgriTrackBackend.CUSTOMER.Customer;
import AgriTrackBackend.CUSTOMER.CustomerRepository;
import AgriTrackBackend.DRIVER.Driver;
import AgriTrackBackend.DRIVER.DriverRepository;
import AgriTrackBackend.EXCEPTION.ForbiddenException;
import AgriTrackBackend.EXCEPTION.ResourceNotFoundException;
import AgriTrackBackend.RATE.Rate;
import AgriTrackBackend.RATE.RateRepository;
import AgriTrackBackend.SECURITY.CurrentUser;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
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

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AuditService auditService;

    // ✅ START a timer-based work session — either the owner working the tractor
    // themselves, or a driver starting a job the owner assigned to them
    @Transactional
    public WorkRecord start(StartWorkRequest req) {
        Long me = CurrentUser.id();
        Long ownerId;

        if (CurrentUser.isRole("DRIVER")) {
            Driver driver = driverRepository.findById(req.getDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
            if (!driver.getUserId().equals(me)) {
                throw new ForbiddenException("You are not this driver");
            }
            // derive the real owner from the driver's profile — never trust the client's ownerId
            ownerId = driver.getOwnerId();
        } else {
            ownerId = me;
        }

        WorkRecord w = new WorkRecord();
        w.setOwnerId(ownerId);
        w.setCustomerId(req.getCustomerId());
        w.setTractorId(req.getTractorId());
        w.setDriverId(req.getDriverId());
        w.setBookingId(req.getBookingId());
        w.setRateId(req.getRateId());
        w.setServiceType(req.getServiceType());
        w.setNotes(req.getNotes());
        w.setWorkDate(LocalDate.now());
        LocalDateTime now = LocalDateTime.now();
        w.setStartTime(now);
        w.setLastResumeTime(now);
        w.setAccumulatedSeconds(0L);
        w.setStatus("RUNNING");
        WorkRecord saved = repository.saveAndFlush(w);
        auditService.log(AuditAction.CREATE, "WorkRecord", saved.getWorkId(), null, saved);
        return saved;
    }

    // owner who booked the job, or the driver actually carrying it out
    private void assertParticipant(WorkRecord w) {
        Long me = CurrentUser.id();
        boolean isOwner = w.getOwnerId() != null && w.getOwnerId().equals(me);
        boolean isDriver = w.getDriverId() != null
                && driverRepository.findById(w.getDriverId()).map(Driver::getUserId).map(me::equals).orElse(false);
        if (!isOwner && !isDriver) {
            throw new ForbiddenException("You do not have access to this work record");
        }
    }

    // ✅ PAUSE — bank elapsed seconds since last resume
    @Transactional
    public WorkRecord pause(Long id) {
        WorkRecord w = getById(id);
        String before = auditService.snapshot(w);
        if ("RUNNING".equals(w.getStatus()) && w.getLastResumeTime() != null) {
            long elapsed = Duration.between(w.getLastResumeTime(), LocalDateTime.now()).getSeconds();
            w.setAccumulatedSeconds(safe(w.getAccumulatedSeconds()) + elapsed);
            w.setLastResumeTime(null);
            w.setStatus("PAUSED");
        }
        WorkRecord saved = repository.saveAndFlush(w);
        auditService.logRaw(AuditAction.STATUS_CHANGE, "WorkRecord", id, before, saved);
        return saved;
    }

    // ✅ RESUME
    @Transactional
    public WorkRecord resume(Long id) {
        WorkRecord w = getById(id);
        String before = auditService.snapshot(w);
        if ("PAUSED".equals(w.getStatus())) {
            w.setLastResumeTime(LocalDateTime.now());
            w.setStatus("RUNNING");
        }
        WorkRecord saved = repository.saveAndFlush(w);
        auditService.logRaw(AuditAction.STATUS_CHANGE, "WorkRecord", id, before, saved);
        return saved;
    }

    // ✅ STOP / COMPLETE — finalize duration & amount
    @Transactional
    public WorkRecord stop(Long id, BigDecimal extraCharges) {
        WorkRecord w = getById(id);
        String before = auditService.snapshot(w);
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
        WorkRecord saved = repository.saveAndFlush(w);
        auditService.logRaw(AuditAction.STATUS_CHANGE, "WorkRecord", id, before, saved);
        return saved;
    }

    // ✅ Manual entry (no live timer) — owner enters duration directly
    @Transactional
    public WorkRecord saveManual(WorkRecord w) {
        w.setOwnerId(CurrentUser.id());
        if (w.getWorkDate() == null) w.setWorkDate(LocalDate.now());
        long seconds = w.getDurationMinutes() != null ? w.getDurationMinutes() * 60 : 0;
        w.setAccumulatedSeconds(seconds);
        BigDecimal amount = calculateAmount(w.getRateId(), seconds);
        if (w.getExtraCharges() != null) {
            amount = amount.add(w.getExtraCharges());
        }
        w.setAmount(amount);
        if (w.getStatus() == null) w.setStatus("COMPLETED");
        WorkRecord saved = repository.saveAndFlush(w);
        auditService.log(AuditAction.CREATE, "WorkRecord", saved.getWorkId(), null, saved);
        return saved;
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
        CurrentUser.requireSelf(ownerId);
        return repository.findByOwnerIdOrderByWorkIdDesc(ownerId);
    }

    public PageResponse<WorkRecord> searchPaged(Long ownerId, String search, String status,
                                                 LocalDate from, LocalDate to,
                                                 Integer page, Integer size, String sortBy, String sortDir) {
        CurrentUser.requireSelf(ownerId);
        Pageable pageable = PaginationUtil.build(page, size, sortBy, sortDir, "createdAt");
        String s = (search == null || search.isBlank()) ? null : search;
        String st = (status == null || status.isBlank()) ? null : status;
        return PageResponse.of(repository.search(ownerId, s, st, from, to, pageable));
    }

    public List<WorkRecord> getByCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
        Long me = CurrentUser.id();
        boolean allowed = (customer.getOwnerId() != null && customer.getOwnerId().equals(me))
                || (customer.getUserId() != null && customer.getUserId().equals(me));
        if (!allowed) {
            throw new ForbiddenException("You do not have access to this customer's work history");
        }
        return repository.findByCustomerIdOrderByWorkIdDesc(customerId);
    }

    public List<WorkRecord> getByDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + driverId));
        Long me = CurrentUser.id();
        if (!driver.getUserId().equals(me) && !driver.getOwnerId().equals(me)) {
            throw new ForbiddenException("You do not have access to this driver's work records");
        }
        return repository.findByDriverIdOrderByWorkIdDesc(driverId);
    }

    public WorkRecord getById(Long id) {
        WorkRecord w = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work record not found with id: " + id));
        assertParticipant(w);
        return w;
    }

    @Transactional
    public void deleteById(Long id) {
        WorkRecord w = getById(id);
        if (!w.getOwnerId().equals(CurrentUser.id())) {
            throw new ForbiddenException("Only the owner can delete a work record");
        }
        String before = auditService.snapshot(w);
        repository.delete(w);
        auditService.logRaw(AuditAction.DELETE, "WorkRecord", id, before, null);
    }
}
