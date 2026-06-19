package AgriTrackBackend.INVOICE;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByOwnerIdOrderByInvoiceIdDesc(Long ownerId);

    List<Invoice> findByCustomerIdOrderByInvoiceIdDesc(Long customerId);

    List<Invoice> findByOwnerIdAndStatus(Long ownerId, String status);
}
