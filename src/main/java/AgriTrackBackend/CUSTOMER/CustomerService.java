package AgriTrackBackend.CUSTOMER;

import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.COMMON.PageResponse;
import AgriTrackBackend.COMMON.PaginationUtil;
import AgriTrackBackend.EXCEPTION.ForbiddenException;
import AgriTrackBackend.EXCEPTION.ResourceNotFoundException;
import AgriTrackBackend.SECURITY.CurrentUser;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository repository;

    @Autowired
    private AuditService auditService;

    @Transactional
    public Customer save(Customer customer) {
        // ✅ a customer record always belongs to the currently authenticated owner —
        // never trust an OWNER_ID sent by the client
        customer.setOwnerId(CurrentUser.id());
        Customer saved = repository.saveAndFlush(customer);
        auditService.log(AuditAction.CREATE, "Customer", saved.getCustomerId(), null, saved);
        return saved;
    }

    public List<Customer> getAll() {
        return repository.findAll();
    }

    public List<Customer> getByOwner(Long ownerId) {
        CurrentUser.requireSelf(ownerId);
        return repository.findByOwnerId(ownerId);
    }

    public List<Customer> searchByOwner(Long ownerId, String name) {
        CurrentUser.requireSelf(ownerId);
        return repository.findByOwnerIdAndNameContainingIgnoreCase(ownerId, name);
    }

    public List<Customer> getByOwnerAndType(Long ownerId, String type) {
        CurrentUser.requireSelf(ownerId);
        return repository.findByOwnerIdAndCustomerType(ownerId, type);
    }

    // ✅ paged + search + filter + sort — additive, doesn't replace the plain-list endpoints above
    public PageResponse<Customer> searchPaged(Long ownerId, String search, String customerType,
                                               Integer page, Integer size, String sortBy, String sortDir) {
        CurrentUser.requireSelf(ownerId);
        Pageable pageable = PaginationUtil.build(page, size, sortBy, sortDir, "createdAt");
        Page<Customer> result = repository.search(ownerId, blankToNull(search), blankToNull(customerType), pageable);
        return PageResponse.of(result);
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    public Customer getById(Long id) {
        Customer customer = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        assertAccess(customer);
        return customer;
    }

    // ✅ only the owning tractor-owner, or the customer's own linked login, may view/edit this row
    private void assertAccess(Customer customer) {
        Long me = CurrentUser.id();
        boolean isOwner = customer.getOwnerId() != null && customer.getOwnerId().equals(me);
        boolean isLinkedCustomer = customer.getUserId() != null && customer.getUserId().equals(me);
        if (!isOwner && !isLinkedCustomer) {
            throw new ForbiddenException("You do not have access to this customer");
        }
    }

    @Transactional
    public Customer update(Long id, Customer data) {
        Customer existing = getById(id);
        String before = auditService.snapshot(existing);
        existing.setName(data.getName());
        existing.setMobileNo(data.getMobileNo());
        existing.setEmail(data.getEmail());
        existing.setAddress(data.getAddress());
        existing.setVillage(data.getVillage());
        existing.setCustomerType(data.getCustomerType());
        existing.setFarmSize(data.getFarmSize());
        existing.setLatitude(data.getLatitude());
        existing.setLongitude(data.getLongitude());
        existing.setPreferredPaymentMethod(data.getPreferredPaymentMethod());
        existing.setPhotoUrl(data.getPhotoUrl());
        existing.setNotes(data.getNotes());
        Customer saved = repository.saveAndFlush(existing);
        auditService.logRaw(AuditAction.UPDATE, "Customer", id, before, saved);
        return saved;
    }

    @Transactional
    public void deleteById(Long id) {
        Customer existing = getById(id); // throws 404/403 as appropriate
        String before = auditService.snapshot(existing);
        repository.delete(existing);
        auditService.logRaw(AuditAction.DELETE, "Customer", id, before, null);
    }
}
