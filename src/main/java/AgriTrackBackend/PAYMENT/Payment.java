package AgriTrackBackend.PAYMENT;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "PAYMENTS")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PAYMENT_ID")
    private Long paymentId;

    @Column(name = "INVOICE_ID")
    private Long invoiceId;

    @Column(name = "WORK_ID")
    private Long workId;

    @Column(name = "BOOKING_ID")
    private Long bookingId;

    @Column(name = "OWNER_ID")
    private Long ownerId;

    @Column(name = "CUSTOMER_ID")
    private Long customerId;

    @Column(name = "AMOUNT", precision = 12, scale = 2)
    private BigDecimal amount;

    // CASH / UPI / CARD / BANK
    @Column(name = "PAYMENT_METHOD", length = 30)
    private String paymentMethod;

    // PENDING / SUCCESS / FAILED
    @Column(name = "PAYMENT_STATUS", length = 20)
    private String paymentStatus;

    @Column(name = "TRANSACTION_ID", length = 100)
    private String transactionId;

    @Column(name = "PAYMENT_DATE")
    private LocalDateTime paymentDate;

    @Column(name = "NOTES", length = 500)
    private String notes;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
