package AgriTrackBackend.GOOGLEAUTH;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleLoginResponse {

    private Boolean success;

    private String message;

    private String token;

    private Long userId;

    private String name;

    private String email;

    private String role;

    // new — enterprise session/refresh support (additive, backward compatible)
    private String refreshToken;

    private long expiresIn;

    private Long sessionId;

    public GoogleLoginResponse(Boolean success, String message, String token, Long userId,
                                String name, String email, String role,
                                String refreshToken, long expiresIn, Long sessionId) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.sessionId = sessionId;
    }
}