package AgriTrackBackend.OTP;

/**
 * The only seam OtpService talks to for actually delivering a code.
 * Swap the implementation (e.g. to a real SMTP/Twilio/MSG91-backed sender)
 * when credentials are available — OtpService, the API contracts, and the
 * dev-mode "return the OTP in the response" behavior never need to change.
 */
public interface OtpSender {
    void send(OtpChannel channel, String destination, String otp, OtpPurpose purpose);
}
