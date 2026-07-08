package AgriTrackBackend.EXPENSE;

import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.COMMON.PageResponse;
import AgriTrackBackend.COMMON.PaginationUtil;
import AgriTrackBackend.EXCEPTION.ForbiddenException;
import AgriTrackBackend.EXCEPTION.ResourceNotFoundException;
import AgriTrackBackend.PAYMENT.Payment;
import AgriTrackBackend.PAYMENT.PaymentRepository;
import AgriTrackBackend.SECURITY.CurrentUser;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository repository;

    // reused, not duplicated — Profit & Loss needs the same "successful
    // payments" revenue definition ReportService already uses
    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AuditService auditService;

    @Transactional
    public Expense create(ExpenseRequest request) {
        Expense expense = fromRequest(new Expense(), request);
        expense.setOwnerId(CurrentUser.id());
        Expense saved = repository.saveAndFlush(expense);
        auditService.log(AuditAction.CREATE, "Expense", saved.getExpenseId(), null, saved);
        return saved;
    }

    public PageResponse<Expense> searchPaged(Long ownerId, String search, String category, Long tractorId,
                                              LocalDate from, LocalDate to,
                                              Integer page, Integer size, String sortBy, String sortDir) {
        CurrentUser.requireSelf(ownerId);
        Pageable pageable = PaginationUtil.build(page, size, sortBy, sortDir, "expenseDate");
        String s = (search == null || search.isBlank()) ? null : search;
        String c = (category == null || category.isBlank()) ? null : category.toUpperCase();
        return PageResponse.of(repository.search(ownerId, s, c, tractorId, from, to, pageable));
    }

    public Expense getById(Long id) {
        Expense expense = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));
        assertOwner(expense);
        return expense;
    }

    private void assertOwner(Expense expense) {
        if (expense.getOwnerId() == null || !expense.getOwnerId().equals(CurrentUser.id())) {
            throw new ForbiddenException("You do not have access to this expense");
        }
    }

    @Transactional
    public Expense update(Long id, ExpenseRequest request) {
        Expense existing = getById(id);
        String before = auditService.snapshot(existing);
        fromRequest(existing, request);
        Expense saved = repository.saveAndFlush(existing);
        auditService.logRaw(AuditAction.UPDATE, "Expense", id, before, saved);
        return saved;
    }

    @Transactional
    public void deleteById(Long id) {
        Expense existing = getById(id);
        String before = auditService.snapshot(existing);
        repository.delete(existing);
        auditService.logRaw(AuditAction.DELETE, "Expense", id, before, null);
    }

    // ✅ Profit & Loss + category/monthly breakdown for a date range
    // (defaults to the last 6 months when no range is given)
    public ProfitLossResponse profitAndLoss(Long ownerId, LocalDate from, LocalDate to) {
        CurrentUser.requireSelf(ownerId);

        LocalDate rangeTo = to != null ? to : LocalDate.now();
        LocalDate rangeFrom = from != null ? from : rangeTo.minusMonths(5).withDayOfMonth(1);

        List<Expense> expenses = repository.findByOwnerIdAndExpenseDateBetween(ownerId, rangeFrom, rangeTo);
        List<Payment> payments = paymentRepository.findByOwnerIdOrderByPaymentIdDesc(ownerId);

        BigDecimal totalExpenses = expenses.stream()
                .map(Expense::getAmount).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> byCategory = new HashMap<>();
        for (String c : ExpenseCategory.ALL) byCategory.put(c, BigDecimal.ZERO);
        for (Expense e : expenses) {
            if (e.getAmount() != null) {
                byCategory.merge(e.getCategory(), e.getAmount(), BigDecimal::add);
            }
        }

        DateTimeFormatter monthKeyFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, BigDecimal> revenueByMonth = new TreeMap<>();
        Map<String, BigDecimal> expenseByMonth = new TreeMap<>();
        YearMonth cursor = YearMonth.from(rangeFrom);
        YearMonth endMonth = YearMonth.from(rangeTo);
        while (!cursor.isAfter(endMonth)) {
            String key = cursor.format(monthKeyFmt);
            revenueByMonth.put(key, BigDecimal.ZERO);
            expenseByMonth.put(key, BigDecimal.ZERO);
            cursor = cursor.plusMonths(1);
        }

        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Payment p : payments) {
            if (!"SUCCESS".equalsIgnoreCase(p.getPaymentStatus()) || p.getAmount() == null || p.getPaymentDate() == null) {
                continue;
            }
            LocalDate paymentDate = p.getPaymentDate().toLocalDate();
            if (paymentDate.isBefore(rangeFrom) || paymentDate.isAfter(rangeTo)) continue;
            totalRevenue = totalRevenue.add(p.getAmount());
            String key = p.getPaymentDate().format(monthKeyFmt);
            if (revenueByMonth.containsKey(key)) revenueByMonth.merge(key, p.getAmount(), BigDecimal::add);
        }
        for (Expense e : expenses) {
            if (e.getAmount() == null || e.getExpenseDate() == null) continue;
            String key = e.getExpenseDate().format(monthKeyFmt);
            if (expenseByMonth.containsKey(key)) expenseByMonth.merge(key, e.getAmount(), BigDecimal::add);
        }

        List<ProfitLossResponse.MonthlyBreakdown> monthly = new ArrayList<>();
        for (String key : revenueByMonth.keySet()) {
            BigDecimal rev = revenueByMonth.get(key);
            BigDecimal exp = expenseByMonth.get(key);
            monthly.add(new ProfitLossResponse.MonthlyBreakdown(key, rev, exp, rev.subtract(exp)));
        }

        return new ProfitLossResponse(
                ownerId, rangeFrom.toString(), rangeTo.toString(),
                totalRevenue, totalExpenses, totalRevenue.subtract(totalExpenses),
                byCategory, monthly
        );
    }

    private Expense fromRequest(Expense expense, ExpenseRequest request) {
        expense.setCategory(request.getCategory() == null ? null : request.getCategory().toUpperCase());
        expense.setAmount(request.getAmount());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setTractorId(request.getTractorId());
        expense.setDriverId(request.getDriverId());
        expense.setPaymentMethod(request.getPaymentMethod());
        expense.setVendor(request.getVendor());
        expense.setDescription(request.getDescription());
        expense.setBillUrl(request.getBillUrl());
        return expense;
    }
}
