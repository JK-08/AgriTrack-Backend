package AgriTrackBackend.PAYMENT;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOwnerIdOrderByPaymentIdDesc(Long ownerId);

    List<Payment> findByCustomerIdOrderByPaymentIdDesc(Long customerId);

    List<Payment> findByOwnerIdAndPaymentStatus(Long ownerId, String paymentStatus);

    List<Payment> findByInvoiceId(Long invoiceId);

    @Query("SELECT p FROM Payment p WHERE p.ownerId = :ownerId " +
            "AND (:search IS NULL OR LOWER(p.transactionId) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR p.paymentStatus = :status) " +
            "AND (:method IS NULL OR p.paymentMethod = :method) " +
            "AND (:from IS NULL OR p.paymentDate >= :from) " +
            "AND (:to IS NULL OR p.paymentDate <= :to)")
    Page<Payment> search(@Param("ownerId") Long ownerId,
                          @Param("search") String search,
                          @Param("status") String status,
                          @Param("method") String method,
                          @Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to,
                          Pageable pageable);
}
