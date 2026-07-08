package AgriTrackBackend.EXPENSE;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "EXPENSES")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EXPENSE_ID")
    private Long expenseId;

    @Column(name = "OWNER_ID", nullable = false)
    private Long ownerId;

    @Column(name = "TRACTOR_ID")
    private Long tractorId;

    @Column(name = "DRIVER_ID")
    private Long driverId;

    // FUEL / REPAIRS / SPARE_PARTS / INSURANCE / DRIVER_SALARY / EMI / TAXES / MISC
    @Column(name = "CATEGORY", length = 30, nullable = false)
    private String category;

    @Column(name = "AMOUNT", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "EXPENSE_DATE", nullable = false)
    private LocalDate expenseDate;

    // CASH / UPI / CARD / BANK
    @Column(name = "PAYMENT_METHOD", length = 30)
    private String paymentMethod;

    @Column(name = "VENDOR", length = 150)
    private String vendor;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "BILL_URL", length = 500)
    private String billUrl;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
