package AgriTrackBackend.SESSION;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Audit trail of every login attempt (successful or not), independent of
 * whether a session/refresh-token was ever issued.
 */
@Getter
@Setter
@Entity
@Table(name = "LOGIN_HISTORY")
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    // nullable: a failed login for an unknown username has no user to attach to
    @Column(name = "USER_ID")
    private Long userId;

    @Column(name = "LOGIN_TIME", nullable = false)
    private LocalDateTime loginTime;

    @Column(name = "LOGOUT_TIME")
    private LocalDateTime logoutTime;

    @Column(name = "IP_ADDRESS")
    private String ipAddress;

    @Column(name = "DEVICE")
    private String device;

    @Column(name = "PLATFORM")
    private String platform;

    // SUCCESS / FAILURE
    @Column(name = "STATUS", nullable = false)
    private String status;

    @Column(name = "FAILURE_REASON")
    private String failureReason;
}
