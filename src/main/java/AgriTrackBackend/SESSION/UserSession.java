package AgriTrackBackend.SESSION;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One row per device/session a user is logged in on. The refresh token is
 * never stored in plaintext — only its SHA-256 hash — so a DB leak can't be
 * replayed as a live session.
 */
@Getter
@Setter
@Entity
@Table(name = "USER_SESSIONS")
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "REFRESH_TOKEN_HASH", nullable = false, unique = true, length = 128)
    private String refreshTokenHash;

    @Column(name = "DEVICE_ID")
    private String deviceId;

    @Column(name = "DEVICE_NAME")
    private String deviceName;

    @Column(name = "PLATFORM")
    private String platform;

    @Column(name = "APP_VERSION")
    private String appVersion;

    @Column(name = "IP_ADDRESS")
    private String ipAddress;

    @Column(name = "LOGIN_TIME", nullable = false)
    private LocalDateTime loginTime;

    @Column(name = "LAST_ACTIVITY", nullable = false)
    private LocalDateTime lastActivity;

    @Column(name = "EXPIRES_AT", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "REVOKED", nullable = false)
    private Boolean revoked = false;

    @Column(name = "LOGOUT_TIME")
    private LocalDateTime logoutTime;
}
