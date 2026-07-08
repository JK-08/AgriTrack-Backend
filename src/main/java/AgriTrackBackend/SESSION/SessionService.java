package AgriTrackBackend.SESSION;

import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.COMMON.PageResponse;
import AgriTrackBackend.COMMON.PaginationUtil;
import AgriTrackBackend.EXCEPTION.ForbiddenException;
import AgriTrackBackend.EXCEPTION.ResourceNotFoundException;
import AgriTrackBackend.EXCEPTION.UnauthorizedException;
import AgriTrackBackend.SECURITY.JwtUtil;
import AgriTrackBackend.USERS.User;
import AgriTrackBackend.USERS.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Owns everything about "how a login turns into tokens" and "how a device
 * stays logged in": issuing access+refresh token pairs, rotating refresh
 * tokens on every use (so a stolen-but-unused refresh token becomes
 * worthless the moment the legitimate client refreshes), tracking one
 * session row per device, and recording login history.
 */
@Service
public class SessionService {

    @Autowired
    private UserSessionRepository sessionRepository;

    @Autowired
    private LoginHistoryRepository loginHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuditService auditService;

    @Value("${jwt.refresh-expiration-days:30}")
    private long refreshExpirationDays;

    @Value("${session.idle-timeout-minutes:20160}")
    private long idleTimeoutMinutes;

    // ------------------------------------------------------------------
    // Login (creates a brand-new session + records history)
    // ------------------------------------------------------------------

    @Transactional
    public AuthResponse createSession(User user, DeviceInfo device, String ip) {
        AuthResponse response = issueTokens(user, device, ip);
        recordLogin(user.getUserId(), ip, device, "SUCCESS", null);
        return response;
    }

    public void recordFailedLogin(Long userId, String ip, DeviceInfo device, String reason) {
        recordLogin(userId, ip, device, "FAILURE", reason);
    }

    private void recordLogin(Long userId, String ip, DeviceInfo device, String status, String reason) {
        LoginHistory history = new LoginHistory();
        history.setUserId(userId);
        history.setLoginTime(LocalDateTime.now());
        history.setIpAddress(ip);
        history.setDevice(device != null ? device.deviceName() : null);
        history.setPlatform(device != null ? device.platform() : null);
        history.setStatus(status);
        history.setFailureReason(reason);
        loginHistoryRepository.save(history);

        // Login Events — never log credentials, only outcome metadata
        String action = "SUCCESS".equals(status) ? AuditAction.LOGIN : AuditAction.LOGIN_FAILED;
        auditService.log(action, "User", userId, null,
                java.util.Map.of("ip", ip == null ? "" : ip, "platform", device != null && device.platform() != null ? device.platform() : ""));
    }

    // ------------------------------------------------------------------
    // Refresh (rotation: old refresh token is revoked, a new pair is issued)
    // ------------------------------------------------------------------

    @Transactional
    public AuthResponse refresh(String rawRefreshToken, DeviceInfo device, String ip) {
        String hash = jwtUtil.hashToken(rawRefreshToken);

        UserSession session = sessionRepository.findByRefreshTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (Boolean.TRUE.equals(session.getRevoked())) {
            // Reuse of an already-rotated/revoked refresh token — could be a stolen
            // token being replayed. Treat as a logged-out device.
            throw new UnauthorizedException("This session has been logged out. Please log in again.");
        }

        LocalDateTime now = LocalDateTime.now();

        if (session.getExpiresAt().isBefore(now)) {
            session.setRevoked(true);
            session.setLogoutTime(now);
            sessionRepository.save(session);
            throw new UnauthorizedException("Refresh token expired. Please log in again.");
        }

        if (session.getLastActivity().plusMinutes(idleTimeoutMinutes).isBefore(now)) {
            session.setRevoked(true);
            session.setLogoutTime(now);
            sessionRepository.save(session);
            throw new UnauthorizedException("Session expired due to inactivity. Please log in again.");
        }

        // ✅ rotation — this refresh token can never be used again
        session.setRevoked(true);
        session.setLogoutTime(now);
        sessionRepository.save(session);

        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return issueTokens(user, device, ip);
    }

    private AuthResponse issueTokens(User user, DeviceInfo device, String ip) {
        String accessToken = jwtUtil.generateToken(user.getUserId(), user.getEmail(), user.getRole().name());
        String rawRefreshToken = jwtUtil.generateRefreshToken();
        String refreshHash = jwtUtil.hashToken(rawRefreshToken);

        LocalDateTime now = LocalDateTime.now();

        UserSession session = new UserSession();
        session.setUserId(user.getUserId());
        session.setRefreshTokenHash(refreshHash);
        session.setDeviceId(device != null ? device.deviceId() : null);
        session.setDeviceName(device != null ? device.deviceName() : null);
        session.setPlatform(device != null ? device.platform() : null);
        session.setAppVersion(device != null ? device.appVersion() : null);
        session.setIpAddress(ip);
        session.setLoginTime(now);
        session.setLastActivity(now);
        session.setExpiresAt(now.plusDays(refreshExpirationDays));
        session.setRevoked(false);
        session = sessionRepository.save(session);

        return new AuthResponse(
                accessToken,
                rawRefreshToken,
                "Bearer",
                jwtUtil.getAccessExpirationMs() / 1000,
                user.getRole().name(),
                user.getName(),
                user.getUserId(),
                session.getId()
        );
    }

    // ------------------------------------------------------------------
    // Logout
    // ------------------------------------------------------------------

    @Transactional
    public void logout(String rawRefreshToken) {
        String hash = jwtUtil.hashToken(rawRefreshToken);
        sessionRepository.findByRefreshTokenHash(hash).ifPresent(session -> {
            LocalDateTime now = LocalDateTime.now();
            session.setRevoked(true);
            session.setLogoutTime(now);
            sessionRepository.save(session);

            // best-effort: close out the matching open login-history row
            List<LoginHistory> open = loginHistoryRepository
                    .findByUserIdAndLogoutTimeIsNullOrderByLoginTimeDesc(session.getUserId());
            if (!open.isEmpty()) {
                LoginHistory latest = open.get(0);
                latest.setLogoutTime(now);
                loginHistoryRepository.save(latest);
            }
            auditService.log(AuditAction.LOGOUT, "UserSession", session.getId(), null, null);
        });
        // Idempotent: an already-revoked/unknown refresh token still returns
        // success — the caller's goal (not being logged in) is already true.
    }

    @Transactional
    public void logoutAll(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<UserSession> sessions = sessionRepository.findByUserIdAndRevokedFalseOrderByLastActivityDesc(userId);
        for (UserSession session : sessions) {
            session.setRevoked(true);
            session.setLogoutTime(now);
        }
        sessionRepository.saveAll(sessions);

        List<LoginHistory> open = loginHistoryRepository
                .findByUserIdAndLogoutTimeIsNullOrderByLoginTimeDesc(userId);
        for (LoginHistory history : open) {
            history.setLogoutTime(now);
        }
        loginHistoryRepository.saveAll(open);

        auditService.log(AuditAction.LOGOUT_ALL, "UserSession", userId, null,
                java.util.Map.of("devicesRevoked", sessions.size()));
    }

    // ------------------------------------------------------------------
    // Session / history listing & management (all ownership-checked by caller id)
    // ------------------------------------------------------------------

    public List<SessionDto> listSessions(Long userId) {
        return sessionRepository.findByUserIdOrderByLoginTimeDesc(userId)
                .stream()
                .map(SessionDto::from)
                .toList();
    }

    public PageResponse<SessionDto> searchSessionsPaged(Long userId, Boolean revoked, String platform,
                                                          Integer page, Integer size, String sortBy, String sortDir) {
        Pageable pageable = PaginationUtil.build(page, size, sortBy, sortDir, "loginTime");
        return PageResponse.of(sessionRepository.search(userId, revoked, platform, pageable).map(SessionDto::from));
    }

    @Transactional
    public void revokeSession(Long userId, Long sessionId) {
        UserSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        if (!session.getUserId().equals(userId)) {
            throw new ForbiddenException("You do not have access to this session");
        }

        session.setRevoked(true);
        session.setLogoutTime(LocalDateTime.now());
        sessionRepository.save(session);
    }

    public List<LoginHistory> loginHistory(Long userId) {
        return loginHistoryRepository.findByUserIdOrderByLoginTimeDesc(userId);
    }

    public PageResponse<LoginHistory> searchLoginHistoryPaged(Long userId, String status, String platform,
                                                                Integer page, Integer size, String sortBy, String sortDir) {
        Pageable pageable = PaginationUtil.build(page, size, sortBy, sortDir, "loginTime");
        return PageResponse.of(loginHistoryRepository.search(userId, status, platform, pageable));
    }
}
