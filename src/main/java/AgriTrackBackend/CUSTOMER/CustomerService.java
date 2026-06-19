package AgriTrackBackend.CUSTOMER;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository repository;

    @Transactional
    public Customer save(Customer customer) {
        return repository.saveAndFlush(customer);
    }

    public List<Customer> getAll() {
        return repository.findAll();
    }

    public List<Customer> getByOwner(Long ownerId) {
        return repository.findByOwnerId(ownerId);
    }

    public List<Customer> searchByOwner(Long ownerId, String name) {
        return repository.findByOwnerIdAndNameContainingIgnoreCase(ownerId, name);
    }

    public List<Customer> getByOwnerAndType(Long ownerId, String type) {
        return repository.findByOwnerIdAndCustomerType(ownerId, type);
    }

    public Customer getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
    }

    @Transactional
    public Customer update(Long id, Customer data) {
        Customer existing = getById(id);
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
        return repository.saveAndFlush(existing);
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
