package AgriTrackBackend.PAYROLL;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class GeneratePayrollRequest {

    @NotNull(message = "driverId is required")
    private Long driverId;

    @NotNull(message = "month is required")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "month must be in yyyy-MM format")
    private String month;

    private BigDecimal incentives = BigDecimal.ZERO;
    private BigDecimal penalties = BigDecimal.ZERO;
    private String notes;
}
