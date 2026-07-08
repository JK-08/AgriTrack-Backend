package AgriTrackBackend.RATE;

import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.COMMON.PageResponse;
import AgriTrackBackend.COMMON.PaginationUtil;
import AgriTrackBackend.CUSTOMER.Customer;
import AgriTrackBackend.CUSTOMER.CustomerRepository;
import AgriTrackBackend.EXCEPTION.ForbiddenException;
import AgriTrackBackend.EXCEPTION.ResourceNotFoundException;
import AgriTrackBackend.NOTIFICATION.NotificationEntity;
import AgriTrackBackend.NOTIFICATION.NotificationRepository;
import AgriTrackBackend.SECURITY.CurrentUser;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RateService {

    @Autowired
    private RateRepository repository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuditService auditService;

    @Transactional
    public Rate save(Rate rate) {
        rate.setOwnerId(CurrentUser.id());
        if (rate.getIsActive() == null) {
            rate.setIsActive(true);
        }
        Rate saved = repository.saveAndFlush(rate);
        auditService.log(AuditAction.CREATE, "Rate", saved.getRateId(), null, saved);
        return saved;
    }

    public List<Rate> getAll() {
        return repository.findAll();
    }

    public List<Rate> getByOwner(Long ownerId) {
        CurrentUser.requireSelf(ownerId);
        return repository.findByOwnerId(ownerId);
    }

    public PageResponse<Rate> searchPaged(Long ownerId, String search, String machineType, Boolean isActive,
                                           Integer page, Integer size, String sortBy, String sortDir) {
        CurrentUser.requireSelf(ownerId);
        Pageable pageable = PaginationUtil.build(page, size, sortBy, sortDir, "createdAt");
        String s = (search == null || search.isBlank()) ? null : search;
        String mt = (machineType == null || machineType.isBlank()) ? null : machineType;
        return PageResponse.of(repository.search(ownerId, s, mt, isActive, pageable));
    }

    // ✅ intentionally open to any authenticated user (farmers need to see an
    // owner's active rates before booking), so no ownership check here
    public List<Rate> getActiveByOwner(Long ownerId) {
        return repository.findByOwnerIdAndIsActiveTrue(ownerId);
    }

    public Rate getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rate not found with id: " + id));
    }

    private void assertOwner(Rate rate) {
        if (rate.getOwnerId() == null || !rate.getOwnerId().equals(CurrentUser.id())) {
            throw new ForbiddenException("You do not have access to this rate");
        }
    }

    @Transactional
    public Rate update(Long id, Rate data) {
        Rate existing = getById(id);
        assertOwner(existing);
        String before = auditService.snapshot(existing);
        boolean priceChanged = priceDiffers(existing.getPricePerMinute(), data.getPricePerMinute())
                || priceDiffers(existing.getPricePerTenMinutes(), data.getPricePerTenMinutes())
                || priceDiffers(existing.getPricePerHour(), data.getPricePerHour());

        existing.setServiceType(data.getServiceType());
        existing.setMachineType(data.getMachineType());
        existing.setPricePerMinute(data.getPricePerMinute());
        existing.setPricePerTenMinutes(data.getPricePerTenMinutes());
        existing.setPricePerHour(data.getPricePerHour());
        if (data.getIsActive() != null) {
            existing.setIsActive(data.getIsActive());
        }
        Rate saved = repository.saveAndFlush(existing);
        auditService.logRaw(AuditAction.UPDATE, "Rate", id, before, saved);

        if (priceChanged) {
            notifyRateChange(saved);
        }
        return saved;
    }

    private boolean priceDiffers(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return false;
        if (a == null || b == null) return true;
        return a.compareTo(b) != 0;
    }

    // ✅ RATE_ALERT — notify every customer of this owner that has a linked USERS login
    private void notifyRateChange(Rate rate) {
        List<Customer> customers = customerRepository.findByOwnerId(rate.getOwnerId());
        for (Customer c : customers) {
            if (c.getUserId() == null) continue;
            NotificationEntity n = new NotificationEntity();
            n.setUserId(c.getUserId());
            n.setTitle("Rate updated: " + rate.getServiceType());
            n.setSubtitle(buildPriceSummary(rate));
            n.setNotificationType("RATE_ALERT");
            n.setScreenName("RateAlerts");
            n.setIsActive(true);
            n.setIsSent(false);
            n.setSendAt(LocalDateTime.now());
            notificationRepository.save(n);
        }
    }

    private String buildPriceSummary(Rate rate) {
        StringBuilder sb = new StringBuilder("New price: ");
        if (rate.getPricePerHour() != null) sb.append("₹").append(rate.getPricePerHour()).append("/hr ");
        if (rate.getPricePerMinute() != null) sb.append("₹").append(rate.getPricePerMinute()).append("/min ");
        if (rate.getPricePerTenMinutes() != null) sb.append("₹").append(rate.getPricePerTenMinutes()).append("/10min");
        return sb.toString().trim();
    }

    @Transactional
    public void deleteById(Long id) {
        Rate existing = getById(id);
        assertOwner(existing);
        String before = auditService.snapshot(existing);
        repository.delete(existing);
        auditService.logRaw(AuditAction.DELETE, "Rate", id, before, null);
    }
}
