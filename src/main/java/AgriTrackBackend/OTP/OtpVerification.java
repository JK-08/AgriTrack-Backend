package AgriTrackBackend.OTP;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "OTP_VERIFICATIONS")
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "IDENTIFIER", nullable = false)
    private String identifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "PURPOSE", nullable = false)
    private OtpPurpose purpose;

    @Column(name = "OTP_HASH", nullable = false, length = 128)
    private String otpHash;

    @Column(name = "EXPIRES_AT", nullable = false)
    private LocalDateTime expiresAt;

    // the OTP itself has been successfully verified/consumed (cannot be verified again)
    @Column(name = "USED", nullable = false)
    private Boolean used = false;

    @Column(name = "ATTEMPT_COUNT", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "MAX_ATTEMPTS", nullable = false)
    private Integer maxAttempts = 5;

    @Column(name = "VERIFIED", nullable = false)
    private Boolean verified = false;

    @Column(name = "VERIFIED_AT")
    private LocalDateTime verifiedAt;

    @Column(name = "RESET_TOKEN_HASH", length = 128)
    private String resetTokenHash;

    @Column(name = "RESET_TOKEN_EXPIRES_AT")
    private LocalDateTime resetTokenExpiresAt;

    @Column(name = "RESET_TOKEN_CONSUMED", nullable = false)
    private Boolean resetTokenConsumed = false;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
