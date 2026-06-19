package AgriTrackBackend.PAYMENT;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOwnerIdOrderByPaymentIdDesc(Long ownerId);

    List<Payment> findByCustomerIdOrderByPaymentIdDesc(Long customerId);

    List<Payment> findByOwnerIdAndPaymentStatus(Long ownerId, String paymentStatus);

    List<Payment> findByInvoiceId(Long invoiceId);
}
