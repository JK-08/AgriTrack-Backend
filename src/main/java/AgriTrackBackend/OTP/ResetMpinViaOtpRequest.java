package AgriTrackBackend.OTP;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetMpinViaOtpRequest {

    @NotBlank(message = "Email or mobile number is required")
    private String identifier;

    @NotBlank(message = "Reset token is required")
    private String resetToken;

    @NotBlank(message = "New MPIN is required")
    @Pattern(regexp = "\\d{4,6}", message = "MPIN must be 4-6 digits")
    private String newMpin;
}
