package AgriTrackBackend.EXCEPTION;

/** Thrown when an authenticated user tries to access/modify a resource they don't own. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
