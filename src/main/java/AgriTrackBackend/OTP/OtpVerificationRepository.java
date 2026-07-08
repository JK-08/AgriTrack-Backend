package AgriTrackBackend.OTP;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    // most recent OTP for this identifier+purpose (used/expired or not) — verify() operates on this
    Optional<OtpVerification> findFirstByIdentifierAndPurposeOrderByCreatedAtDesc(String identifier, OtpPurpose purpose);

    // most recent *verified* OTP with a live reset token — reset() operates on this
    Optional<OtpVerification> findFirstByIdentifierAndPurposeAndVerifiedTrueOrderByCreatedAtDesc(String identifier, OtpPurpose purpose);

    long countByIdentifierAndPurposeAndCreatedAtAfter(String identifier, OtpPurpose purpose, LocalDateTime after);

    List<OtpVerification> findByIdentifierAndPurpose(String identifier, OtpPurpose purpose);
}
