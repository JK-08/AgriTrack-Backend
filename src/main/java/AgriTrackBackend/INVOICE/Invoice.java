package AgriTrackBackend.INVOICE;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "INVOICES")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "INVOICE_ID")
    private Long invoiceId;

    @Column(name = "INVOICE_NUMBER", length = 50)
    private String invoiceNumber;

    @Column(name = "OWNER_ID", nullable = false)
    private Long ownerId;

    @Column(name = "CUSTOMER_ID")
    private Long customerId;

    @Column(name = "WORK_ID")
    private Long workId;

    @Column(name = "BOOKING_ID")
    private Long bookingId;

    @Column(name = "SUBTOTAL", precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "EXTRA_CHARGES", precision = 12, scale = 2)
    private BigDecimal extraCharges;

    @Column(name = "TAX", precision = 12, scale = 2)
    private BigDecimal tax;

    @Column(name = "TOTAL_AMOUNT", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    // PAID / UNPAID / PARTIAL
    @Column(name = "STATUS", length = 20)
    private String status;

    @Column(name = "INVOICE_DATE")
    private LocalDate invoiceDate;

    @Column(name = "PDF_URL", length = 500)
    private String pdfUrl;

    @Column(name = "NOTES", length = 500)
    private String notes;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
