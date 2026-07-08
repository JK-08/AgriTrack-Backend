package AgriTrackBackend.OTP;

import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.EXCEPTION.ResourceNotFoundException;
import AgriTrackBackend.EXCEPTION.TooManyRequestsException;
import AgriTrackBackend.EXCEPTION.UnauthorizedException;
import AgriTrackBackend.MPIN.Mpin;
import AgriTrackBackend.MPIN.MpinRepository;
import AgriTrackBackend.SESSION.SessionService;
import AgriTrackBackend.USERS.User;
import AgriTrackBackend.USERS.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Forgot Password / Forgot MPIN via OTP — one implementation shared by both
 * flows (OtpPurpose distinguishes them). Three steps, each its own API call:
 *   1) sendOtp    — generate + "deliver" a 6-digit code, 5-minute expiry
 *   2) verifyOtp  — check the code, issue a short-lived one-time reset token
 *   3) reset*     — spend the reset token to actually change the credential
 *
 * Splitting verify from reset means the OTP can never be replayed to change
 * the credential a second time, and the reset token is meaningless without
 * having passed OTP verification first.
 */
@Service
public class OtpService {

    @Autowired
    private OtpVerificationRepository otpRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MpinRepository mpinRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private OtpSender otpSender;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private AuditService auditService;

    @Value("${app.otp.dev-mode:true}")
    private boolean devMode;

    @Value("${app.otp.expiry-minutes:5}")
    private long expiryMinutes;

    @Value("${app.otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.otp.max-resend-per-window:3}")
    private int maxResendPerWindow;

    @Value("${app.otp.resend-window-minutes:15}")
    private long resendWindowMinutes;

    @Value("${app.otp.reset-token-expiry-minutes:10}")
    private long resetTokenExpiryMinutes;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // ------------------------------------------------------------------
    // Step 1 — send
    // ------------------------------------------------------------------

    @Transactional
    public OtpResponse sendOtp(String identifier, OtpPurpose purpose) {

        // Don't reveal whether the identifier is registered — return a
        // generic success either way, but only actually issue an OTP if a
        // matching user exists (and, for MPIN reset, has an MPIN set).
        User user = userRepository.findByEmailOrMobileNo(identifier, identifier).orElse(null);
        boolean eligible = user != null && (purpose != OtpPurpose.MPIN_RESET || mpinRepository.findByUserUserId(user.getUserId()).isPresent());

        if (!eligible) {
            return new OtpResponse("If this account exists, an OTP has been sent.", expiryMinutes * 60, null);
        }

        long recentCount = otpRepository.countByIdentifierAndPurposeAndCreatedAtAfter(
                identifier, purpose, LocalDateTime.now().minusMinutes(resendWindowMinutes));

        if (recentCount >= maxResendPerWindow) {
            throw new TooManyRequestsException(
                    "Too many OTP requests. Please wait before requesting another OTP.");
        }

        String otp = generateSixDigitOtp();

        OtpVerification record = new OtpVerification();
        record.setIdentifier(identifier);
        record.setPurpose(purpose);
        record.setOtpHash(hash(otp));
        record.setExpiresAt(LocalDateTime.now().plusMinutes(expiryMinutes));
        record.setUsed(false);
        record.setAttemptCount(0);
        record.setMaxAttempts(maxAttempts);
        record.setVerified(false);
        record.setResetTokenConsumed(false);
        otpRepository.save(record);

        OtpChannel channel = identifier.contains("@") ? OtpChannel.EMAIL : OtpChannel.SMS;
        otpSender.send(channel, identifier, otp, purpose);

        return new OtpResponse(
                "OTP sent successfully.",
                expiryMinutes * 60,
                devMode ? otp : null
        );
    }

    // ------------------------------------------------------------------
    // Step 2 — verify
    // ------------------------------------------------------------------

    @Transactional
    public VerifyOtpResponse verifyOtp(String identifier, OtpPurpose purpose, String otp) {

        OtpVerification record = otpRepository
                .findFirstByIdentifierAndPurposeOrderByCreatedAtDesc(identifier, purpose)
                .orElseThrow(() -> new UnauthorizedException("No OTP request found for this account"));

        if (Boolean.TRUE.equals(record.getUsed())) {
            throw new UnauthorizedException("This OTP has already been used. Please request a new one.");
        }

        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("OTP has expired. Please request a new one.");
        }

        if (record.getAttemptCount() >= record.getMaxAttempts()) {
            record.setUsed(true); // lock this OTP out entirely
            otpRepository.save(record);
            throw new TooManyRequestsException("Too many incorrect attempts. Please request a new OTP.");
        }

        if (!record.getOtpHash().equals(hash(otp))) {
            record.setAttemptCount(record.getAttemptCount() + 1);
            otpRepository.save(record);
            throw new UnauthorizedException("Invalid OTP");
        }

        // ✅ correct — consume the OTP and issue a one-time reset token
        String rawResetToken = generateOpaqueToken();

        record.setUsed(true);
        record.setVerified(true);
        record.setVerifiedAt(LocalDateTime.now());
        record.setResetTokenHash(hash(rawResetToken));
        record.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(resetTokenExpiryMinutes));
        record.setResetTokenConsumed(false);
        otpRepository.save(record);

        return new VerifyOtpResponse(
                "OTP verified successfully.",
                rawResetToken,
                resetTokenExpiryMinutes * 60
        );
    }

    // ------------------------------------------------------------------
    // Step 3a — reset password
    // ------------------------------------------------------------------

    @Transactional
    public void resetPassword(String identifier, String resetToken, String newPassword) {
        User user = userRepository.findByEmailOrMobileNo(identifier, identifier)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        consumeResetToken(identifier, OtpPurpose.PASSWORD_RESET, resetToken);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        auditService.log(AuditAction.PASSWORD_RESET, "User", user.getUserId(), null, null); // never log password material

        // password changed — invalidate every existing session/device as a
        // precaution (whoever reset it should be the only one still signed in)
        sessionService.logoutAll(user.getUserId());
    }

    // ------------------------------------------------------------------
    // Step 3b — reset MPIN
    // ------------------------------------------------------------------

    @Transactional
    public void resetMpin(String identifier, String resetToken, String newMpin) {
        User user = userRepository.findByEmailOrMobileNo(identifier, identifier)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Mpin mpin = mpinRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("MPIN not found"));

        consumeResetToken(identifier, OtpPurpose.MPIN_RESET, resetToken);

        mpin.setMpin(passwordEncoder.encode(newMpin));
        mpinRepository.save(mpin);
        auditService.log(AuditAction.MPIN_RESET, "Mpin", user.getUserId(), null, null); // never log MPIN material

        sessionService.logoutAll(user.getUserId());
    }

    private void consumeResetToken(String identifier, OtpPurpose purpose, String resetToken) {
        OtpVerification record = otpRepository
                .findFirstByIdentifierAndPurposeAndVerifiedTrueOrderByCreatedAtDesc(identifier, purpose)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired reset token"));

        if (record.getResetTokenHash() == null || Boolean.TRUE.equals(record.getResetTokenConsumed())) {
            throw new UnauthorizedException("This reset token has already been used");
        }

        if (record.getResetTokenExpiresAt() == null || record.getResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Reset token has expired. Please verify OTP again.");
        }

        if (!record.getResetTokenHash().equals(hash(resetToken))) {
            throw new UnauthorizedException("Invalid reset token");
        }

        record.setResetTokenConsumed(true);
        otpRepository.save(record);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private String generateSixDigitOtp() {
        int code = 100000 + SECURE_RANDOM.nextInt(900000); // 100000-999999, always 6 digits
        return String.valueOf(code);
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] out = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
