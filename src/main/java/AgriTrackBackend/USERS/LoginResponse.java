package AgriTrackBackend.USERS;

import lombok.Getter;
import lombok.Setter;

/**
 * Backward-compatible: existing fields (token, role, name, userId) are
 * unchanged in name/position so old app builds parsing this response keep
 * working. refreshToken/expiresIn/sessionId are additive.
 */
@Getter
@Setter
public class LoginResponse {

    private String token;
    private String role;
    private String name;
    private Long userId;

    // new — enterprise session/refresh support
    private String refreshToken;
    private long expiresIn;
    private Long sessionId;

    public LoginResponse(String token, String role, String name, Long userId) {
        this.token = token;
        this.role = role;
        this.name = name;
        this.userId = userId;
    }

    public LoginResponse(String token, String role, String name, Long userId,
                          String refreshToken, long expiresIn, Long sessionId) {
        this.token = token;
        this.role = role;
        this.name = name;
        this.userId = userId;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.sessionId = sessionId;
    }
}
