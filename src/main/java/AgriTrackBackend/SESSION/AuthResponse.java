package AgriTrackBackend.SESSION;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;   // "Bearer"
    private long expiresIn;     // seconds until the access token expires
    private String role;
    private String name;
    private Long userId;
    private Long sessionId;
}
