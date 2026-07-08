package AgriTrackBackend.INVOICE;

import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.CUSTOMER.CustomerRepository;
import AgriTrackBackend.EXCEPTION.ForbiddenException;
import AgriTrackBackend.SECURITY.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Invoice was previously the biggest gap flagged by the Phase-1 review: it
 * had ownership checks but zero audit wiring even though "Payment Events"
 * were explicitly in scope. This covers CREATE/UPDATE/STATUS_CHANGE/DELETE
 * auditing plus the pre-existing ownership authorization around them.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository repository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private InvoiceService service;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(Long userId, String role) {
        AuthenticatedUser principal = new AuthenticatedUser(userId, "user" + userId + "@example.com", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @Test
    void saveComputesTotalDefaultsStatusAndAuditsCreate() {
        loginAs(1L, "OWNER");
        Invoice invoice = new Invoice();
        invoice.setSubtotal(new BigDecimal("100.00"));
        invoice.setExtraCharges(new BigDecimal("10.00"));
        invoice.setTax(new BigDecimal("5.00"));
        when(repository.saveAndFlush(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            i.setInvoiceId(20L);
            return i;
        });

        Invoice saved = service.save(invoice);

        assertThat(saved.getOwnerId()).isEqualTo(1L);
        assertThat(saved.getStatus()).isEqualTo("UNPAID");
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("115.00");
        verify(auditService).log(eq(AuditAction.CREATE), eq("Invoice"), eq(20L), eq(null), eq(saved));
    }

    @Test
    void updateRejectsNonIssuingOwner() {
        loginAs(2L, "OWNER");
        Invoice existing = new Invoice();
        existing.setInvoiceId(20L);
        existing.setOwnerId(1L);
        when(repository.findById(20L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(20L, new Invoice()))
                .isInstanceOf(ForbiddenException.class);
        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void updateByIssuingOwnerAuditsUpdateWithBeforeSnapshot() {
        loginAs(1L, "OWNER");
        Invoice existing = new Invoice();
        existing.setInvoiceId(20L);
        existing.setOwnerId(1L);
        existing.setStatus("UNPAID");
        when(repository.findById(20L)).thenReturn(Optional.of(existing));
        when(auditService.snapshot(existing)).thenReturn("{\"status\":\"UNPAID\"}");
        when(repository.saveAndFlush(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice data = new Invoice();
        data.setStatus("PARTIAL");
        data.setSubtotal(BigDecimal.TEN);
        service.update(20L, data);

        verify(auditService).logRaw(eq(AuditAction.UPDATE), eq("Invoice"), eq(20L),
                eq("{\"status\":\"UNPAID\"}"), any());
    }

    @Test
    void updateStatusAuditsAsStatusChangeNotPlainUpdate() {
        loginAs(1L, "OWNER");
        Invoice existing = new Invoice();
        existing.setInvoiceId(20L);
        existing.setOwnerId(1L);
        existing.setStatus("UNPAID");
        when(repository.findById(20L)).thenReturn(Optional.of(existing));
        when(auditService.snapshot(existing)).thenReturn("{\"status\":\"UNPAID\"}");
        when(repository.saveAndFlush(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice result = service.updateStatus(20L, "PAID");

        assertThat(result.getStatus()).isEqualTo("PAID");
        verify(auditService).logRaw(eq(AuditAction.STATUS_CHANGE), eq("Invoice"), eq(20L), any(), any());
    }

    @Test
    void deleteByIdRejectsNonIssuingOwnerAndNeverAudits() {
        loginAs(2L, "OWNER");
        Invoice existing = new Invoice();
        existing.setInvoiceId(20L);
        existing.setOwnerId(1L);
        when(repository.findById(20L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.deleteById(20L)).isInstanceOf(ForbiddenException.class);
        verify(repository, never()).delete(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void deleteByIdByIssuingOwnerAuditsDelete() {
        loginAs(1L, "OWNER");
        Invoice existing = new Invoice();
        existing.setInvoiceId(20L);
        existing.setOwnerId(1L);
        when(repository.findById(20L)).thenReturn(Optional.of(existing));
        when(auditService.snapshot(existing)).thenReturn("{}");

        service.deleteById(20L);

        verify(repository).delete(existing);
        verify(auditService).logRaw(eq(AuditAction.DELETE), eq("Invoice"), eq(20L), eq("{}"), eq(null));
    }
}
