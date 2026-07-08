package AgriTrackBackend.PAYMENT;

import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.CUSTOMER.Customer;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Payment was the other explicitly-named "Payment Events" gap from the
 * Phase-1 audit review — previously zero AuditService wiring. Also covers
 * that no card/bank secret fields exist on the entity to accidentally leak
 * into audit before/after snapshots.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository repository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PaymentService service;

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
    void saveByOwnerStampsOwnerIdDefaultsStatusAndAuditsCreate() {
        loginAs(1L, "OWNER");
        Payment payment = new Payment();
        when(repository.saveAndFlush(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setPaymentId(30L);
            return p;
        });

        Payment saved = service.save(payment);

        assertThat(saved.getOwnerId()).isEqualTo(1L);
        assertThat(saved.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(saved.getPaymentDate()).isNotNull();
        verify(auditService).log(eq(AuditAction.CREATE), eq("Payment"), eq(30L), eq(null), eq(saved));
    }

    @Test
    void saveByCustomerVerifiesCustomerAccessBeforeAuditing() {
        loginAs(5L, "CUSTOMER");
        Customer customer = new Customer();
        customer.setUserId(5L);
        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer));
        Payment payment = new Payment();
        payment.setCustomerId(7L);
        when(repository.saveAndFlush(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        service.save(payment);

        verify(customerRepository).findById(7L);
        verify(auditService).log(eq(AuditAction.CREATE), eq("Payment"), any(), eq(null), any());
    }

    @Test
    void saveByCustomerRejectsUnauthorizedCustomerAndNeverAudits() {
        loginAs(5L, "CUSTOMER");
        Customer customer = new Customer();
        customer.setUserId(99L); // different customer
        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer));
        Payment payment = new Payment();
        payment.setCustomerId(7L);

        assertThatThrownBy(() -> service.save(payment)).isInstanceOf(ForbiddenException.class);
        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void updateStatusAuditsAsStatusChangeWithBeforeSnapshot() {
        loginAs(1L, "OWNER");
        Payment existing = new Payment();
        existing.setPaymentId(30L);
        existing.setOwnerId(1L);
        existing.setPaymentStatus("PENDING");
        when(repository.findById(30L)).thenReturn(Optional.of(existing));
        when(auditService.snapshot(existing)).thenReturn("{\"paymentStatus\":\"PENDING\"}");
        when(repository.saveAndFlush(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = service.updateStatus(30L, "SUCCESS");

        assertThat(result.getPaymentStatus()).isEqualTo("SUCCESS");
        verify(auditService).logRaw(eq(AuditAction.STATUS_CHANGE), eq("Payment"), eq(30L),
                eq("{\"paymentStatus\":\"PENDING\"}"), any());
    }

    @Test
    void deleteByIdRejectsNonOwnerAndNeverAudits() {
        loginAs(2L, "OWNER");
        Payment existing = new Payment();
        existing.setPaymentId(30L);
        existing.setOwnerId(1L);
        when(repository.findById(30L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.deleteById(30L)).isInstanceOf(ForbiddenException.class);
        verify(repository, never()).delete(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void deleteByIdByOwnerAuditsDelete() {
        loginAs(1L, "OWNER");
        Payment existing = new Payment();
        existing.setPaymentId(30L);
        existing.setOwnerId(1L);
        when(repository.findById(30L)).thenReturn(Optional.of(existing));
        when(auditService.snapshot(existing)).thenReturn("{}");

        service.deleteById(30L);

        verify(repository).delete(existing);
        verify(auditService).logRaw(eq(AuditAction.DELETE), eq("Payment"), eq(30L), eq("{}"), eq(null));
    }
}
