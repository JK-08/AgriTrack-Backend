package AgriTrackBackend.EXCEPTION;

/**
 * Thrown for authentication failures that are distinct from "no token at
 * all" — e.g. an expired/revoked refresh token, a logged-out device, or a
 * session that timed out from inactivity. Maps to HTTP 401.
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
