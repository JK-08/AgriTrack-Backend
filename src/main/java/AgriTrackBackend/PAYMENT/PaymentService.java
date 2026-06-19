package AgriTrackBackend.PAYMENT;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository repository;

    @Transactional
    public Payment save(Payment payment) {
        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDateTime.now());
        }
        if (payment.getPaymentStatus() == null) {
            payment.setPaymentStatus("SUCCESS");
        }
        return repository.saveAndFlush(payment);
    }

    public List<Payment> getAll() {
        return repository.findAll();
    }

    public List<Payment> getByOwner(Long ownerId) {
        return repository.findByOwnerIdOrderByPaymentIdDesc(ownerId);
    }

    public List<Payment> getByCustomer(Long customerId) {
        return repository.findByCustomerIdOrderByPaymentIdDesc(customerId);
    }

    public List<Payment> getPending(Long ownerId) {
        return repository.findByOwnerIdAndPaymentStatus(ownerId, "PENDING");
    }

    public List<Payment> getByInvoice(Long invoiceId) {
        return repository.findByInvoiceId(invoiceId);
    }

    public Payment getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + id));
    }

    @Transactional
    public Payment updateStatus(Long id, String status) {
        Payment existing = getById(id);
        existing.setPaymentStatus(status);
        return repository.saveAndFlush(existing);
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
