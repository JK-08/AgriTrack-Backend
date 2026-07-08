package AgriTrackBackend.EXPENSE;

/** Fixed set of expense categories the product supports today. */
public final class ExpenseCategory {
    public static final String FUEL = "FUEL";
    public static final String REPAIRS = "REPAIRS";
    public static final String SPARE_PARTS = "SPARE_PARTS";
    public static final String INSURANCE = "INSURANCE";
    public static final String DRIVER_SALARY = "DRIVER_SALARY";
    public static final String EMI = "EMI";
    public static final String TAXES = "TAXES";
    public static final String MISC = "MISC";

    public static final java.util.Set<String> ALL = java.util.Set.of(
            FUEL, REPAIRS, SPARE_PARTS, INSURANCE, DRIVER_SALARY, EMI, TAXES, MISC);

    private ExpenseCategory() {}

    public static boolean isValid(String value) {
        return value != null && ALL.contains(value.toUpperCase());
    }
}
