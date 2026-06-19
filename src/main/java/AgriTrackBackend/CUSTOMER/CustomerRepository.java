package AgriTrackBackend.CUSTOMER;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByOwnerId(Long ownerId);

    List<Customer> findByOwnerIdAndCustomerType(Long ownerId, String customerType);

    List<Customer> findByOwnerIdAndNameContainingIgnoreCase(Long ownerId, String name);
}
