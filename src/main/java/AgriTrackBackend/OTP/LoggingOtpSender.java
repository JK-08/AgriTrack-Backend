package AgriTrackBackend.OTP;

import org.springframework.stereotype.Component;

/**
 * Placeholder OtpSender used because no SMS/Email provider is integrated
 * yet (per explicit product decision — see DEV_OTP_MODE). Logs the OTP
 * server-side instead of delivering it externally. The API response
 * separately echoes the OTP back to the caller when app.otp.dev-mode=true
 * (see OtpService) so the mobile app can show it in an Alert for testing.
 *
 * To go live: implement OtpSender with a real SMTP/SMS client and either
 * replace this @Component or make it conditional on a profile/property —
 * no other class in the OTP flow needs to change.
 */
@Component
public class LoggingOtpSender implements OtpSender {

    @Override
    public void send(OtpChannel channel, String destination, String otp, OtpPurpose purpose) {
        System.out.println(
                "[OTP] channel=" + channel +
                " purpose=" + purpose +
                " destination=" + destination +
                " otp=" + otp +
                " (no SMS/Email provider configured — logged only)"
        );
    }
}
