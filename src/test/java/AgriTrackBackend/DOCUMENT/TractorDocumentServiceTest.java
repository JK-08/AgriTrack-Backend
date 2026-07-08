package AgriTrackBackend.DOCUMENT;

import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.EXCEPTION.ForbiddenException;
import AgriTrackBackend.NOTIFICATION.NotificationEntity;
import AgriTrackBackend.NOTIFICATION.NotificationRepository;
import AgriTrackBackend.SECURITY.AuthenticatedUser;
import AgriTrackBackend.TRACTOR.Tractor;
import AgriTrackBackend.TRACTOR.TractorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TractorDocumentServiceTest {

    @Mock private TractorDocumentRepository repository;
    @Mock private TractorRepository tractorRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private TractorDocumentService service;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(Long userId) {
        AuthenticatedUser principal = new AuthenticatedUser(userId, "owner@example.com", "OWNER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private Tractor tractor(Long id, Long ownerId) {
        Tractor t = new Tractor();
        t.setTractorId(id);
        t.setOwnerId(ownerId);
        return t;
    }

    private TractorDocumentRequest request(Long tractorId, String type, LocalDate expiry) {
        TractorDocumentRequest r = new TractorDocumentRequest();
        r.setTractorId(tractorId);
        r.setDocumentType(type);
        r.setFileUrl("https://cdn/doc.pdf");
        r.setExpiryDate(expiry);
        return r;
    }

    @Test
    void uploadRejectsRequestForAnotherOwnersTractor() {
        loginAs(1L);
        when(tractorRepository.findById(20L)).thenReturn(Optional.of(tractor(20L, 2L)));

        assertThatThrownBy(() -> service.upload(request(20L, "RC", LocalDate.now().plusYears(1))))
                .isInstanceOf(ForbiddenException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void uploadCreatesNewDocumentAndAuditsCreate() {
        loginAs(1L);
        when(tractorRepository.findById(20L)).thenReturn(Optional.of(tractor(20L, 1L)));
        when(repository.findByTractorIdAndDocumentType(20L, "RC")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(TractorDocument.class))).thenAnswer(inv -> {
            TractorDocument d = inv.getArgument(0);
            d.setDocumentId(300L);
            return d;
        });

        TractorDocument result = service.upload(request(20L, "rc", LocalDate.now().plusYears(1)));

        assertThat(result.getDocumentType()).isEqualTo("RC");
        assertThat(result.getOwnerId()).isEqualTo(1L);
        assertThat(result.getReminderSent()).isFalse();
        verify(auditService).logRaw(eq(AuditAction.CREATE), eq("TractorDocument"), eq(300L), eq(null), eq(result));
    }

    @Test
    void uploadReplacesExistingDocumentOfSameTypeAndResetsReminder() {
        loginAs(1L);
        when(tractorRepository.findById(20L)).thenReturn(Optional.of(tractor(20L, 1L)));

        TractorDocument existing = new TractorDocument();
        existing.setDocumentId(300L);
        existing.setTractorId(20L);
        existing.setOwnerId(1L);
        existing.setDocumentType("INSURANCE");
        existing.setFileUrl("https://cdn/old.pdf");
        existing.setReminderSent(true);
        when(repository.findByTractorIdAndDocumentType(20L, "INSURANCE")).thenReturn(Optional.of(existing));
        when(auditService.snapshot(existing)).thenReturn("{\"fileUrl\":\"old\"}");
        when(repository.saveAndFlush(any(TractorDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        TractorDocument result = service.upload(request(20L, "INSURANCE", LocalDate.now().plusMonths(6)));

        assertThat(result.getFileUrl()).isEqualTo("https://cdn/doc.pdf");
        assertThat(result.getReminderSent()).isFalse(); // reset on replace
        verify(auditService).logRaw(eq(AuditAction.UPDATE), eq("TractorDocument"), eq(300L),
                eq("{\"fileUrl\":\"old\"}"), eq(result));
    }

    @Test
    void getByTractorRejectsNonOwner() {
        loginAs(2L);
        when(tractorRepository.findById(20L)).thenReturn(Optional.of(tractor(20L, 1L)));

        assertThatThrownBy(() -> service.getByTractor(20L)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deleteByIdRejectsNonOwner() {
        loginAs(2L);
        TractorDocument existing = new TractorDocument();
        existing.setDocumentId(300L);
        existing.setOwnerId(1L);
        when(repository.findById(300L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.deleteById(300L)).isInstanceOf(ForbiddenException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void deleteByIdByOwnerAuditsDelete() {
        loginAs(1L);
        TractorDocument existing = new TractorDocument();
        existing.setDocumentId(300L);
        existing.setOwnerId(1L);
        when(repository.findById(300L)).thenReturn(Optional.of(existing));
        when(auditService.snapshot(existing)).thenReturn("{}");

        service.deleteById(300L);

        verify(repository).delete(existing);
        verify(auditService).logRaw(eq(AuditAction.DELETE), eq("TractorDocument"), eq(300L), eq("{}"), eq(null));
    }

    @Test
    void sendExpiryRemindersCreatesNotificationAndMarksReminderSent() {
        TractorDocument expiring = new TractorDocument();
        expiring.setDocumentId(300L);
        expiring.setOwnerId(1L);
        expiring.setTractorId(20L);
        expiring.setDocumentType("PUC");
        expiring.setExpiryDate(LocalDate.now().plusDays(5));
        when(repository.findByExpiryDateBetweenAndReminderSentFalse(any(), any())).thenReturn(List.of(expiring));

        service.sendExpiryReminders();

        ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getNotificationType()).isEqualTo("DOCUMENT_EXPIRY");
        assertThat(expiring.getReminderSent()).isTrue();
        verify(repository).save(expiring);
    }

    @Test
    void sendExpiryRemindersDoesNothingWhenNoneAreExpiring() {
        when(repository.findByExpiryDateBetweenAndReminderSentFalse(any(), any())).thenReturn(List.of());

        service.sendExpiryReminders();

        verifyNoInteractions(notificationRepository);
    }
}
