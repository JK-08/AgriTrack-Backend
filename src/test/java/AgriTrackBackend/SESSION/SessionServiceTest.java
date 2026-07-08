package AgriTrackBackend.SESSION;

import AgriTrackBackend.EXCEPTION.ForbiddenException;
import AgriTrackBackend.EXCEPTION.UnauthorizedException;
import AgriTrackBackend.SECURITY.JwtUtil;
import AgriTrackBackend.USERS.Role;
import AgriTrackBackend.USERS.User;
import AgriTrackBackend.USERS.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Covers login → session creation, refresh rotation, replay/expiry/idle
 * rejection, logout, logout-all across multiple devices, and ownership
 * checks on session revocation — the core guarantees of the JWT
 * refresh-token + session-management feature.
 */
@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private UserSessionRepository sessionRepository;

    @Mock
    private LoginHistoryRepository loginHistoryRepository;

    @Mock
    private UserRepository userRepository;

    private JwtUtil jwtUtil;

    @InjectMocks
    private SessionService sessionService;

    private User user;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "TEST_SECRET_TEST_SECRET_TEST_SECRET_32B");
        ReflectionTestUtils.setField(jwtUtil, "accessExpirationMs", 900000L);
        ReflectionTestUtils.setField(sessionService, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(sessionService, "refreshExpirationDays", 30L);
        ReflectionTestUtils.setField(sessionService, "idleTimeoutMinutes", 20160L);

        user = new User();
        user.setUserId(1L);
        user.setEmail("owner@example.com");
        user.setName("Test Owner");
        user.setRole(Role.OWNER);

        // repository.save just echoes back what it was given, with an id assigned
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(inv -> {
            UserSession s = inv.getArgument(0);
            if (s.getId() == null) s.setId(100L);
            return s;
        });
    }

    @Test
    void loginCreatesSessionAndRecordsHistory() {
        AuthResponse response = sessionService.createSession(user, sampleDevice(), "10.0.0.1");

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getUserId()).isEqualTo(1L);

        verify(sessionRepository).save(any(UserSession.class));
        verify(loginHistoryRepository).save(argThat(h -> "SUCCESS".equals(h.getStatus())));
    }

    @Test
    void refreshRotatesTokenAndRevokesOldOne() {
        UserSession existing = activeSession("old-hash");
        when(sessionRepository.findByRefreshTokenHash(any())).thenReturn(Optional.of(existing));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        AuthResponse response = sessionService.refresh("raw-refresh-token", sampleDevice(), "10.0.0.1");

        assertThat(existing.getRevoked()).isTrue();
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotEqualTo("raw-refresh-token");
    }

    @Test
    void refreshRejectsAlreadyRevokedToken_replayAttack() {
        UserSession revoked = activeSession("hash");
        revoked.setRevoked(true);
        when(sessionRepository.findByRefreshTokenHash(any())).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> sessionService.refresh("stolen-token", sampleDevice(), "10.0.0.1"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refreshRejectsExpiredToken() {
        UserSession expired = activeSession("hash");
        expired.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(sessionRepository.findByRefreshTokenHash(any())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> sessionService.refresh("token", sampleDevice(), "10.0.0.1"))
                .isInstanceOf(UnauthorizedException.class);
        assertThat(expired.getRevoked()).isTrue();
    }

    @Test
    void refreshRejectsIdleSession() {
        UserSession idle = activeSession("hash");
        idle.setLastActivity(LocalDateTime.now().minusDays(60));
        when(sessionRepository.findByRefreshTokenHash(any())).thenReturn(Optional.of(idle));

        assertThatThrownBy(() -> sessionService.refresh("token", sampleDevice(), "10.0.0.1"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refreshRejectsUnknownToken() {
        when(sessionRepository.findByRefreshTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.refresh("bogus", sampleDevice(), "10.0.0.1"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void logoutRevokesMatchingSession() {
        UserSession session = activeSession("hash");
        when(sessionRepository.findByRefreshTokenHash(any())).thenReturn(Optional.of(session));
        when(loginHistoryRepository.findByUserIdAndLogoutTimeIsNullOrderByLoginTimeDesc(1L))
                .thenReturn(List.of());

        sessionService.logout("raw-token");

        assertThat(session.getRevoked()).isTrue();
        assertThat(session.getLogoutTime()).isNotNull();
    }

    @Test
    void logoutIsIdempotentForUnknownToken() {
        when(sessionRepository.findByRefreshTokenHash(any())).thenReturn(Optional.empty());
        // should not throw
        sessionService.logout("already-used-or-unknown");
    }

    @Test
    void logoutAllRevokesEveryActiveDeviceForUser() {
        UserSession device1 = activeSession("h1");
        UserSession device2 = activeSession("h2");
        when(sessionRepository.findByUserIdAndRevokedFalseOrderByLastActivityDesc(1L))
                .thenReturn(List.of(device1, device2));
        when(loginHistoryRepository.findByUserIdAndLogoutTimeIsNullOrderByLoginTimeDesc(1L))
                .thenReturn(List.of());

        sessionService.logoutAll(1L);

        assertThat(device1.getRevoked()).isTrue();
        assertThat(device2.getRevoked()).isTrue();
        verify(sessionRepository).saveAll(List.of(device1, device2));
    }

    @Test
    void revokeSessionRejectsCrossUserAccess() {
        UserSession othersSession = activeSession("h");
        othersSession.setId(55L);
        othersSession.setUserId(2L); // belongs to a different user
        when(sessionRepository.findById(55L)).thenReturn(Optional.of(othersSession));

        assertThatThrownBy(() -> sessionService.revokeSession(1L, 55L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void revokeSessionSucceedsForOwner() {
        UserSession mine = activeSession("h");
        mine.setId(55L);
        mine.setUserId(1L);
        when(sessionRepository.findById(55L)).thenReturn(Optional.of(mine));

        sessionService.revokeSession(1L, 55L);

        assertThat(mine.getRevoked()).isTrue();
    }

    private UserSession activeSession(String hash) {
        UserSession s = new UserSession();
        s.setId(1L);
        s.setUserId(1L);
        s.setRefreshTokenHash(hash);
        s.setRevoked(false);
        s.setLoginTime(LocalDateTime.now().minusDays(1));
        s.setLastActivity(LocalDateTime.now().minusMinutes(5));
        s.setExpiresAt(LocalDateTime.now().plusDays(29));
        return s;
    }

    private DeviceInfo sampleDevice() {
        return new DeviceInfo("device-abc", "Pixel 8", "android", "1.0.0");
    }
}
