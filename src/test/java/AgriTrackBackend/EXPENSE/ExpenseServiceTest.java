package AgriTrackBackend.EXPENSE;

import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.EXCEPTION.ForbiddenException;
import AgriTrackBackend.PAYMENT.Payment;
import AgriTrackBackend.PAYMENT.PaymentRepository;
import AgriTrackBackend.SECURITY.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository repository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ExpenseService service;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(Long userId) {
        AuthenticatedUser principal = new AuthenticatedUser(userId, "owner@example.com", "OWNER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private ExpenseRequest request(String category, String amount, LocalDate date) {
        ExpenseRequest r = new ExpenseRequest();
        r.setCategory(category);
        r.setAmount(new BigDecimal(amount));
        r.setExpenseDate(date);
        return r;
    }

    @Test
    void createStampsOwnerFromCallerUppercasesCategoryAndAuditsCreate() {
        loginAs(1L);
        when(repository.saveAndFlush(any(Expense.class))).thenAnswer(inv -> {
            Expense e = inv.getArgument(0);
            e.setExpenseId(50L);
            return e;
        });

        Expense saved = service.create(request("fuel", "1500.00", LocalDate.now()));

        assertThat(saved.getOwnerId()).isEqualTo(1L);
        assertThat(saved.getCategory()).isEqualTo("FUEL");
        verify(auditService).log(eq(AuditAction.CREATE), eq("Expense"), eq(50L), eq(null), eq(saved));
    }

    @Test
    void searchPagedRejectsRequestForAnotherOwnersExpenses() {
        loginAs(1L);
        assertThatThrownBy(() -> service.searchPaged(2L, null, null, null, null, null, null, null, null, null))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void searchPagedNormalizesBlankFiltersAndUppercasesCategory() {
        loginAs(1L);
        when(repository.search(eq(1L), eq(null), eq("FUEL"), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.searchPaged(1L, "  ", "fuel", null, null, null, null, null, null, null);

        verify(repository).search(eq(1L), eq(null), eq("FUEL"), eq(null), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    void updateRejectsNonOwnerAndNeverAudits() {
        loginAs(2L);
        Expense existing = new Expense();
        existing.setExpenseId(50L);
        existing.setOwnerId(1L);
        when(repository.findById(50L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(50L, request("FUEL", "100", LocalDate.now())))
                .isInstanceOf(ForbiddenException.class);
        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void updateByOwnerAuditsUpdateWithBeforeSnapshot() {
        loginAs(1L);
        Expense existing = new Expense();
        existing.setExpenseId(50L);
        existing.setOwnerId(1L);
        existing.setCategory("FUEL");
        existing.setAmount(new BigDecimal("100"));
        when(repository.findById(50L)).thenReturn(Optional.of(existing));
        when(auditService.snapshot(existing)).thenReturn("{\"amount\":100}");
        when(repository.saveAndFlush(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        Expense result = service.update(50L, request("REPAIRS", "250.50", LocalDate.now()));

        assertThat(result.getCategory()).isEqualTo("REPAIRS");
        assertThat(result.getAmount()).isEqualByComparingTo("250.50");
        verify(auditService).logRaw(eq(AuditAction.UPDATE), eq("Expense"), eq(50L), eq("{\"amount\":100}"), eq(result));
    }

    @Test
    void deleteByIdRejectsNonOwner() {
        loginAs(2L);
        Expense existing = new Expense();
        existing.setExpenseId(50L);
        existing.setOwnerId(1L);
        when(repository.findById(50L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.deleteById(50L)).isInstanceOf(ForbiddenException.class);
        verify(repository, never()).delete(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void deleteByIdByOwnerAuditsDelete() {
        loginAs(1L);
        Expense existing = new Expense();
        existing.setExpenseId(50L);
        existing.setOwnerId(1L);
        when(repository.findById(50L)).thenReturn(Optional.of(existing));
        when(auditService.snapshot(existing)).thenReturn("{}");

        service.deleteById(50L);

        verify(repository).delete(existing);
        verify(auditService).logRaw(eq(AuditAction.DELETE), eq("Expense"), eq(50L), eq("{}"), eq(null));
    }

    @Test
    void profitAndLossRejectsRequestForAnotherOwner() {
        loginAs(1L);
        assertThatThrownBy(() -> service.profitAndLoss(2L, null, null)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void profitAndLossSubtractsExpensesFromSuccessfulPaymentRevenue() {
        loginAs(1L);
        LocalDate from = LocalDate.now().minusDays(10);
        LocalDate to = LocalDate.now();

        Expense fuel = new Expense();
        fuel.setCategory("FUEL");
        fuel.setAmount(new BigDecimal("300"));
        fuel.setExpenseDate(to);
        Expense repair = new Expense();
        repair.setCategory("REPAIRS");
        repair.setAmount(new BigDecimal("200"));
        repair.setExpenseDate(to);
        when(repository.findByOwnerIdAndExpenseDateBetween(1L, from, to)).thenReturn(List.of(fuel, repair));

        Payment success = payment("1000", "SUCCESS", LocalDateTime.now());
        Payment pending = payment("500", "PENDING", LocalDateTime.now());
        Payment outsideRange = payment("999", "SUCCESS", LocalDateTime.now().minusDays(30));
        when(paymentRepository.findByOwnerIdOrderByPaymentIdDesc(1L)).thenReturn(List.of(success, pending, outsideRange));

        ProfitLossResponse result = service.profitAndLoss(1L, from, to);

        assertThat(result.getTotalRevenue()).isEqualByComparingTo("1000");
        assertThat(result.getTotalExpenses()).isEqualByComparingTo("500");
        assertThat(result.getNetProfit()).isEqualByComparingTo("500");
        assertThat(result.getExpensesByCategory().get("FUEL")).isEqualByComparingTo("300");
        assertThat(result.getExpensesByCategory().get("REPAIRS")).isEqualByComparingTo("200");
        assertThat(result.getExpensesByCategory().get("MISC")).isEqualByComparingTo("0");
    }

    private Payment payment(String amount, String status, LocalDateTime date) {
        Payment p = new Payment();
        p.setAmount(new BigDecimal(amount));
        p.setPaymentStatus(status);
        p.setPaymentDate(date);
        return p;
    }
}
