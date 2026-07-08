package AgriTrackBackend.EXPENSE;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Validated create/update payload for an expense. Kept separate from the
 * {@link Expense} entity so ownerId (always taken from the JWT) can never be
 * supplied by the client, and so the amount/category rules are enforced
 * before anything touches the repository.
 */
@Getter
@Setter
public class ExpenseRequest {

    @NotBlank(message = "Category is required")
    @Pattern(regexp = "FUEL|REPAIRS|SPARE_PARTS|INSURANCE|DRIVER_SALARY|EMI|TAXES|MISC",
            message = "Category must be one of FUEL, REPAIRS, SPARE_PARTS, INSURANCE, DRIVER_SALARY, EMI, TAXES, MISC")
    private String category;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Expense date is required")
    private LocalDate expenseDate;

    private Long tractorId;
    private Long driverId;
    private String paymentMethod;
    private String vendor;
    private String description;
    private String billUrl;
}
