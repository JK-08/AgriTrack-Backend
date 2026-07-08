package AgriTrackBackend.OTP;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyOtpRequest {

    @NotBlank(message = "Email or mobile number is required")
    private String identifier;

    @NotBlank(message = "OTP is required")
    private String otp;
}
