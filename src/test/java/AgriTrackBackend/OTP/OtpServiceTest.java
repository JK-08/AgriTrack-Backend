package AgriTrackBackend.OTP;

import AgriTrackBackend.EXCEPTION.TooManyRequestsException;
import AgriTrackBackend.EXCEPTION.UnauthorizedException;
import AgriTrackBackend.MPIN.MpinRepository;
import AgriTrackBackend.SESSION.SessionService;
import AgriTrackBackend.USERS.Role;
import AgriTrackBackend.USERS.User;
import AgriTrackBackend.USERS.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Covers the full send -> verify -> reset lifecycle: correct OTP succeeds
 * and issues a reset token, wrong OTP increments attempts, exhausted
 * attempts / expiry lock the OTP out, resend is rate-limited, and a reset
 * token can only be spent once.
 */
@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock private OtpVerificationRepository otpRepository;
    @Mock private UserRepository userRepository;
    @Mock private MpinRepository mpinRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private OtpSender otpSender;
    @Mock private SessionService sessionService;

    private OtpService otpService;
    private User user;

    @BeforeEach
    void setUp() {
        otpService = new OtpService();
        ReflectionTestUtils.setField(otpService, "otpRepository", otpRepository);
        ReflectionTestUtils.setField(otpService, "userRepository", userRepository);
        ReflectionTestUtils.setField(otpService, "mpinRepository", mpinRepository);
        ReflectionTestUtils.setField(otpService, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(otpService, "otpSender", otpSender);
        ReflectionTestUtils.setField(otpService, "sessionService", sessionService);
        ReflectionTestUtils.setField(otpService, "devMode", true);
        ReflectionTestUtils.setField(otpService, "expiryMinutes", 5L);
        ReflectionTestUtils.setField(otpService, "maxAttempts", 5);
        ReflectionTestUtils.setField(otpService, "maxResendPerWindow", 3);
        ReflectionTestUtils.setField(otpService, "resendWindowMinutes", 15L);
        ReflectionTestUtils.setField(otpService, "resetTokenExpiryMinutes", 10L);

        user = new User();
        user.setUserId(1L);
        user.setEmail("owner@example.com");
        user.setRole(Role.OWNER);

        when(otpRepository.save(any(OtpVerification.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void sendOtpReturnsOtpInDevModeAndPersistsHashedRecord() {
        when(userRepository.findByEmailOrMobileNo("owner@example.com", "owner@example.com"))
                .thenReturn(Optional.of(user));

        OtpResponse response = otpService.sendOtp("owner@example.com", OtpPurpose.PASSWORD_RESET);

        assertThat(response.getOtp()).matches("\\d{6}");
        verify(otpSender).send(eq(OtpChannel.EMAIL), eq("owner@example.com"), eq(response.getOtp()), eq(OtpPurpose.PASSWORD_RESET));

        ArgumentCaptor<OtpVerification> captor = ArgumentCaptor.forClass(OtpVerification.class);
        verify(otpRepository).save(captor.capture());
        assertThat(captor.getValue().getOtpHash()).isNotEqualTo(response.getOtp()); // never stored raw
    }

    @Test
    void sendOtpDoesNotRevealWhetherAccountExists() {
        when(userRepository.findByEmailOrMobileNo(any(), any())).thenReturn(Optional.empty());

        OtpResponse response = otpService.sendOtp("nobody@example.com", OtpPurpose.PASSWORD_RESET);

        assertThat(response.getOtp()).isNull();
        verify(otpRepository, never()).save(any());
        verify(otpSender, never()).send(any(), any(), any(), any());
    }

    @Test
    void sendOtpIsRateLimitedAfterResendWindowExceeded() {
        when(userRepository.findByEmailOrMobileNo(any(), any())).thenReturn(Optional.of(user));
        when(otpRepository.countByIdentifierAndPurposeAndCreatedAtAfter(any(), any(), any())).thenReturn(3L);

        assertThatThrownBy(() -> otpService.sendOtp("owner@example.com", OtpPurpose.PASSWORD_RESET))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void verifyOtpSucceedsAndIssuesResetToken() {
        when(userRepository.findByEmailOrMobileNo(any(), any())).thenReturn(Optional.of(user));
        OtpResponse sendResponse = otpService.sendOtp("owner@example.com", OtpPurpose.PASSWORD_RESET);
        OtpVerification saved = captureLastSaved();

        when(otpRepository.findFirstByIdentifierAndPurposeOrderByCreatedAtDesc("owner@example.com", OtpPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(saved));

        VerifyOtpResponse response = otpService.verifyOtp("owner@example.com", OtpPurpose.PASSWORD_RESET, sendResponse.getOtp());

        assertThat(response.getResetToken()).isNotBlank();
        assertThat(saved.getUsed()).isTrue();
        assertThat(saved.getVerified()).isTrue();
        assertThat(saved.getResetTokenHash()).isNotEqualTo(response.getResetToken()); // never stored raw
    }

    @Test
    void verifyOtpRejectsWrongCodeAndIncrementsAttempts() {
        when(userRepository.findByEmailOrMobileNo(any(), any())).thenReturn(Optional.of(user));
        otpService.sendOtp("owner@example.com", OtpPurpose.PASSWORD_RESET);
        OtpVerification saved = captureLastSaved();
        when(otpRepository.findFirstByIdentifierAndPurposeOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(saved));

        assertThatThrownBy(() -> otpService.verifyOtp("owner@example.com", OtpPurpose.PASSWORD_RESET, "000000"))
                .isInstanceOf(UnauthorizedException.class);

        assertThat(saved.getAttemptCount()).isEqualTo(1);
        assertThat(saved.getUsed()).isFalse();
    }

    @Test
    void verifyOtpLocksOutAfterMaxAttempts() {
        when(userRepository.findByEmailOrMobileNo(any(), any())).thenReturn(Optional.of(user));
        otpService.sendOtp("owner@example.com", OtpPurpose.PASSWORD_RESET);
        OtpVerification saved = captureLastSaved();
        saved.setAttemptCount(5);
        saved.setMaxAttempts(5);
        when(otpRepository.findFirstByIdentifierAndPurposeOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(saved));

        assertThatThrownBy(() -> otpService.verifyOtp("owner@example.com", OtpPurpose.PASSWORD_RESET, "000000"))
                .isInstanceOf(TooManyRequestsException.class);
        assertThat(saved.getUsed()).isTrue();
    }

    @Test
    void verifyOtpRejectsExpiredOtp() {
        when(userRepository.findByEmailOrMobileNo(any(), any())).thenReturn(Optional.of(user));
        OtpResponse sendResponse = otpService.sendOtp("owner@example.com", OtpPurpose.PASSWORD_RESET);
        OtpVerification saved = captureLastSaved();
        saved.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(otpRepository.findFirstByIdentifierAndPurposeOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(saved));

        assertThatThrownBy(() -> otpService.verifyOtp("owner@example.com", OtpPurpose.PASSWORD_RESET, sendResponse.getOtp()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void resetPasswordRejectsAlreadyConsumedToken() {
        when(userRepository.findByEmailOrMobileNo(any(), any())).thenReturn(Optional.of(user));

        OtpVerification verified = new OtpVerification();
        verified.setIdentifier("owner@example.com");
        verified.setPurpose(OtpPurpose.PASSWORD_RESET);
        verified.setVerified(true);
        verified.setResetTokenHash("some-hash");
        verified.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(5));
        verified.setResetTokenConsumed(true); // already spent

        when(otpRepository.findFirstByIdentifierAndPurposeAndVerifiedTrueOrderByCreatedAtDesc(
                "owner@example.com", OtpPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(verified));

        assertThatThrownBy(() -> otpService.resetPassword("owner@example.com", "any-token", "newPassword123"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void resetPasswordSucceedsAndRevokesAllSessions() {
        when(userRepository.findByEmailOrMobileNo(any(), any())).thenReturn(Optional.of(user));
        OtpResponse sendResponse = otpService.sendOtp("owner@example.com", OtpPurpose.PASSWORD_RESET);
        OtpVerification saved = captureLastSaved();
        // move the fixture through verifyOtp() first so resetTokenHash is a real hash of a real token
        when(otpRepository.findFirstByIdentifierAndPurposeOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(saved));
        VerifyOtpResponse verifyResponse = otpService.verifyOtp("owner@example.com", OtpPurpose.PASSWORD_RESET, sendResponse.getOtp());

        when(otpRepository.findFirstByIdentifierAndPurposeAndVerifiedTrueOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(saved));
        when(passwordEncoder.encode(any())).thenReturn("hashed-password");

        otpService.resetPassword("owner@example.com", verifyResponse.getResetToken(), "newPassword123");

        verify(userRepository).save(user);
        verify(sessionService).logoutAll(1L);
        assertThat(saved.getResetTokenConsumed()).isTrue();
    }

    private OtpVerification captureLastSaved() {
        ArgumentCaptor<OtpVerification> captor = ArgumentCaptor.forClass(OtpVerification.class);
        verify(otpRepository, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }
}
