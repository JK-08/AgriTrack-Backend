package AgriTrackBackend.EXCEPTION;

/** Thrown when a requested resource (by id) does not exist. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
