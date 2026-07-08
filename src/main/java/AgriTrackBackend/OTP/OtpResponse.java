package AgriTrackBackend.OTP;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class OtpResponse {
    private String message;
    private long expiresInSeconds;

    // Only populated when app.otp.dev-mode=true (no live SMS/Email provider
    // configured). NEVER populated in production once a real sender is wired in.
    private String otp;
}
