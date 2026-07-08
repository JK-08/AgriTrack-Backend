package AgriTrackBackend.EXCEPTION;

/** Rate limit exceeded (e.g. OTP resend/verify attempts). Maps to HTTP 429. */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
