package AgriTrackBackend.CUSTOMER;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByOwnerId(Long ownerId);

    List<Customer> findByOwnerIdAndCustomerType(Long ownerId, String customerType);

    List<Customer> findByOwnerIdAndNameContainingIgnoreCase(Long ownerId, String name);

    // Paged + search (name/village/mobile) + filter (customerType) — all optional,
    // pass null to skip a filter. Backs GET /customer/search-paged/{ownerId}.
    @Query("SELECT c FROM Customer c WHERE c.ownerId = :ownerId " +
            "AND (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(c.village) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR c.mobileNo LIKE CONCAT('%', :search, '%')) " +
            "AND (:customerType IS NULL OR c.customerType = :customerType)")
    Page<Customer> search(@Param("ownerId") Long ownerId,
                           @Param("search") String search,
                           @Param("customerType") String customerType,
                           Pageable pageable);
}
