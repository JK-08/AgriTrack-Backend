package AgriTrackBackend.NOTIFICATION;

import AgriTrackBackend.AUDIT.AuditService;
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
import static org.mockito.Mockito.when;

/**
 * Module 6 — verifies notification preference defaults, upsert (create vs
 * update) behavior, and the push-eligibility check consumed by
 * NotificationService before sending FCM pushes.
 */
@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock private NotificationPreferenceRepository repository;
    @Mock private AuditService auditService;

    @InjectMocks
    private NotificationPreferenceService service;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(Long userId) {
        AuthenticatedUser principal = new AuthenticatedUser(userId, "user@example.com", "OWNER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @Test
    void getForUserRejectsRequestForAnotherUser() {
        loginAs(1L);
        assertThatThrownBy(() -> service.getForUser(2L)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getForUserSynthesizesDefaultsForUncustomizedTypes() {
        loginAs(1L);
        when(repository.findByUserId(1L)).thenReturn(List.of());

        List<NotificationPreference> result = service.getForUser(1L);

        assertThat(result).hasSize(NotificationType.ALL.size());
        assertThat(result).allMatch(p -> p.getPushEnabled() && p.getInAppEnabled()
                && !p.getSmsEnabled() && !p.getEmailEnabled());
    }

    @Test
    void getForUserUsesCustomizedRowWhenPresent() {
        loginAs(1L);
        NotificationPreference custom = new NotificationPreference();
        custom.setPreferenceId(5L);
        custom.setUserId(1L);
        custom.setNotificationType(NotificationType.RATE_ALERT);
        custom.setPushEnabled(false);
        custom.setInAppEnabled(true);
        when(repository.findByUserId(1L)).thenReturn(List.of(custom));

        List<NotificationPreference> result = service.getForUser(1L);

        NotificationPreference rateAlert = result.stream()
                .filter(p -> p.getNotificationType().equals(NotificationType.RATE_ALERT))
                .findFirst().orElseThrow();
        assertThat(rateAlert.getPreferenceId()).isEqualTo(5L);
        assertThat(rateAlert.getPushEnabled()).isFalse();
    }

    @Test
    void upsertCreatesNewPreferenceWhenNoneExists() {
        loginAs(1L);
        when(repository.findByUserIdAndNotificationType(1L, NotificationType.BOOKING)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any())).thenAnswer(inv -> {
            NotificationPreference p = inv.getArgument(0);
            p.setPreferenceId(9L);
            return p;
        });

        NotificationPreferenceRequest request = new NotificationPreferenceRequest();
        request.setNotificationType(NotificationType.BOOKING);
        request.setPushEnabled(false);
        request.setInAppEnabled(true);

        NotificationPreference saved = service.upsert(1L, request);

        assertThat(saved.getPreferenceId()).isEqualTo(9L);
        assertThat(saved.getPushEnabled()).isFalse();
        assertThat(saved.getNotificationType()).isEqualTo(NotificationType.BOOKING);
    }

    @Test
    void isPushEnabledDefaultsTrueWhenNoPreferenceExists() {
        when(repository.findByUserIdAndNotificationType(1L, NotificationType.PAYMENT)).thenReturn(Optional.empty());
        assertThat(service.isPushEnabled(1L, NotificationType.PAYMENT)).isTrue();
    }

    @Test
    void isPushEnabledReflectsStoredPreference() {
        NotificationPreference pref = new NotificationPreference();
        pref.setPushEnabled(false);
        when(repository.findByUserIdAndNotificationType(1L, NotificationType.PAYMENT)).thenReturn(Optional.of(pref));
        assertThat(service.isPushEnabled(1L, NotificationType.PAYMENT)).isFalse();
    }
}
