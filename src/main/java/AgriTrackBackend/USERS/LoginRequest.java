package AgriTrackBackend.USERS;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    // email OR mobile
    @NotBlank(message = "Email or mobile number is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    // OWNER / DRIVER / CUSTOMER
    @NotBlank(message = "Role is required")
    private String role;
}