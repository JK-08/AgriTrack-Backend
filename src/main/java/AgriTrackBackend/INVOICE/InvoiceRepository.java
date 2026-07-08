package AgriTrackBackend.INVOICE;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByOwnerIdOrderByInvoiceIdDesc(Long ownerId);

    List<Invoice> findByCustomerIdOrderByInvoiceIdDesc(Long customerId);

    List<Invoice> findByOwnerIdAndStatus(Long ownerId, String status);

    @Query("SELECT i FROM Invoice i WHERE i.ownerId = :ownerId " +
            "AND (:search IS NULL OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR i.status = :status) " +
            "AND (:from IS NULL OR i.invoiceDate >= :from) " +
            "AND (:to IS NULL OR i.invoiceDate <= :to)")
    Page<Invoice> search(@Param("ownerId") Long ownerId,
                          @Param("search") String search,
                          @Param("status") String status,
                          @Param("from") LocalDate from,
                          @Param("to") LocalDate to,
                          Pageable pageable);
}
