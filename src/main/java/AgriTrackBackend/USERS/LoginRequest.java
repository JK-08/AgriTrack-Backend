package AgriTrackBackend.USERS;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    // email OR mobile
    private String username;

    private String password;

    // OWNER / DRIVER / CUSTOMER
    private String role;
}