package AgriTrackBackend.INVOICE;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository repository;

    @Transactional
    public Invoice save(Invoice invoice) {
        if (invoice.getInvoiceDate() == null) {
            invoice.setInvoiceDate(LocalDate.now());
        }
        if (invoice.getInvoiceNumber() == null) {
            invoice.setInvoiceNumber(generateInvoiceNumber());
        }
        invoice.setTotalAmount(computeTotal(invoice));
        if (invoice.getStatus() == null) {
            invoice.setStatus("UNPAID");
        }
        return repository.saveAndFlush(invoice);
    }

    private BigDecimal computeTotal(Invoice inv) {
        BigDecimal total = nz(inv.getSubtotal())
                .add(nz(inv.getExtraCharges()))
                .add(nz(inv.getTax()));
        return total;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private String generateInvoiceNumber() {
        return "INV-" + System.currentTimeMillis();
    }

    public List<Invoice> getAll() {
        return repository.findAll();
    }

    public List<Invoice> getByOwner(Long ownerId) {
        return repository.findByOwnerIdOrderByInvoiceIdDesc(ownerId);
    }

    public List<Invoice> getByCustomer(Long customerId) {
        return repository.findByCustomerIdOrderByInvoiceIdDesc(customerId);
    }

    public Invoice getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));
    }

    @Transactional
    public Invoice update(Long id, Invoice data) {
        Invoice existing = getById(id);
        existing.setCustomerId(data.getCustomerId());
        existing.setWorkId(data.getWorkId());
        existing.setBookingId(data.getBookingId());
        existing.setSubtotal(data.getSubtotal());
        existing.setExtraCharges(data.getExtraCharges());
        existing.setTax(data.getTax());
        existing.setStatus(data.getStatus());
        existing.setPdfUrl(data.getPdfUrl());
        existing.setNotes(data.getNotes());
        existing.setTotalAmount(computeTotal(existing));
        return repository.saveAndFlush(existing);
    }

    @Transactional
    public Invoice updateStatus(Long id, String status) {
        Invoice existing = getById(id);
        existing.setStatus(status);
        return repository.saveAndFlush(existing);
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
