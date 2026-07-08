package AgriTrackBackend.EXPENSE;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Profit & Loss + expense analytics payload for a date range. Revenue comes
 * from the existing PAYMENTS table (SUCCESS only) — this DTO never
 * duplicates that aggregation logic, it just composes it alongside expenses.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfitLossResponse {

    private Long ownerId;
    private String from;
    private String to;

    private BigDecimal totalRevenue;
    private BigDecimal totalExpenses;
    private BigDecimal netProfit;

    // category -> total amount
    private Map<String, BigDecimal> expensesByCategory;

    // "yyyy-MM" -> { revenue, expenses, profit }
    private List<MonthlyBreakdown> monthlyBreakdown;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyBreakdown {
        private String month;
        private BigDecimal revenue;
        private BigDecimal expenses;
        private BigDecimal profit;
    }
}
