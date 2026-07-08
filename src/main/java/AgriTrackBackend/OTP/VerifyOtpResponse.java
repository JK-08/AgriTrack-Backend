package AgriTrackBackend.OTP;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class VerifyOtpResponse {
    private String message;
    private String resetToken;
    private long resetTokenExpiresInSeconds;
}
