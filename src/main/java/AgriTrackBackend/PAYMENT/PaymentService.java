package AgriTrackBackend.PAYMENT;

import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.COMMON.PageResponse;
import AgriTrackBackend.COMMON.PaginationUtil;
import AgriTrackBackend.CUSTOMER.Customer;
import AgriTrackBackend.CUSTOMER.CustomerRepository;
import AgriTrackBackend.EXCEPTION.ForbiddenException;
import AgriTrackBackend.EXCEPTION.ResourceNotFoundException;
import AgriTrackBackend.SECURITY.CurrentUser;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository repository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AuditService auditService;

    @Transactional
    public Payment save(Payment payment) {
        // ✅ whichever side (owner recording cash, or customer paying online) is
        // making the call, pin *their own* identity field — the counterpart id
        // (a reference to the other party) is still trusted from the request.
        if (CurrentUser.isRole("OWNER")) {
            payment.setOwnerId(CurrentUser.id());
        } else if (CurrentUser.isRole("CUSTOMER")) {
            assertCustomerAccess(payment.getCustomerId());
        }
        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDateTime.now());
        }
        if (payment.getPaymentStatus() == null) {
            payment.setPaymentStatus("SUCCESS");
        }
        Payment saved = repository.saveAndFlush(payment);
        // Payment Event — audit metadata only, never card/bank details
        auditService.log(AuditAction.CREATE, "Payment", saved.getPaymentId(), null, saved);
        return saved;
    }

    public List<Payment> getAll() {
        return repository.findAll();
    }

    public List<Payment> getByOwner(Long ownerId) {
        CurrentUser.requireSelf(ownerId);
        return repository.findByOwnerIdOrderByPaymentIdDesc(ownerId);
    }

    public PageResponse<Payment> searchPaged(Long ownerId, String search, String status, String method,
                                              LocalDateTime from, LocalDateTime to,
                                              Integer page, Integer size, String sortBy, String sortDir) {
        CurrentUser.requireSelf(ownerId);
        Pageable pageable = PaginationUtil.build(page, size, sortBy, sortDir, "createdAt");
        String s = (search == null || search.isBlank()) ? null : search;
        String st = (status == null || status.isBlank()) ? null : status;
        String m = (method == null || method.isBlank()) ? null : method;
        return PageResponse.of(repository.search(ownerId, s, st, m, from, to, pageable));
    }

    public List<Payment> getByCustomer(Long customerId) {
        assertCustomerAccess(customerId);
        return repository.findByCustomerIdOrderByPaymentIdDesc(customerId);
    }

    // owner of the customer, or the customer's own linked login
    private void assertCustomerAccess(Long customerId) {
        if (customerId == null) return;
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
        Long me = CurrentUser.id();
        boolean allowed = (customer.getOwnerId() != null && customer.getOwnerId().equals(me))
                || (customer.getUserId() != null && customer.getUserId().equals(me));
        if (!allowed) {
            throw new ForbiddenException("You do not have access to this customer's payments");
        }
    }

    public List<Payment> getPending(Long ownerId) {
        CurrentUser.requireSelf(ownerId);
        return repository.findByOwnerIdAndPaymentStatus(ownerId, "PENDING");
    }

    public List<Payment> getByInvoice(Long invoiceId) {
        // caller is trusted here only if they can also see the payments they're asking for
        List<Payment> payments = repository.findByInvoiceId(invoiceId);
        Long me = CurrentUser.id();
        payments.forEach(p -> {
            boolean allowed = (p.getOwnerId() != null && p.getOwnerId().equals(me))
                    || (p.getCustomerId() != null && me.equals(p.getCustomerId()));
            if (!allowed) assertCustomerAccess(p.getCustomerId());
        });
        return payments;
    }

    public Payment getById(Long id) {
        Payment payment = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        assertAccess(payment);
        return payment;
    }

    private void assertAccess(Payment payment) {
        Long me = CurrentUser.id();
        if (payment.getOwnerId() != null && payment.getOwnerId().equals(me)) return;
        if (payment.getCustomerId() != null) {
            Customer customer = customerRepository.findById(payment.getCustomerId()).orElse(null);
            if (customer != null && customer.getUserId() != null && customer.getUserId().equals(me)) return;
        }
        throw new ForbiddenException("You do not have access to this payment");
    }

    @Transactional
    public Payment updateStatus(Long id, String status) {
        Payment existing = getById(id);
        String before = auditService.snapshot(existing);
        existing.setPaymentStatus(status);
        Payment saved = repository.saveAndFlush(existing);
        auditService.logRaw(AuditAction.STATUS_CHANGE, "Payment", id, before, saved);
        return saved;
    }

    @Transactional
    public void deleteById(Long id) {
        Payment existing = getById(id);
        if (existing.getOwnerId() == null || !existing.getOwnerId().equals(CurrentUser.id())) {
            throw new ForbiddenException("Only the owner can delete a payment record");
        }
        String before = auditService.snapshot(existing);
        repository.delete(existing);
        auditService.logRaw(AuditAction.DELETE, "Payment", id, before, null);
    }
}
