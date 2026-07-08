package AgriTrackBackend.EXCEPTION;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ✅ Bean validation failures (@Valid on @RequestBody DTOs) — one entry per invalid field
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                fieldErrors.put(fe.getField(), fe.getDefaultMessage())
        );

        Map<String, Object> body = baseBody(HttpStatus.BAD_REQUEST, "Validation failed", request);
        body.put("fieldErrors", fieldErrors);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // ✅ Not authenticated (missing/invalid token on a protected route)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex, WebRequest request) {
        return new ResponseEntity<>(baseBody(HttpStatus.UNAUTHORIZED, "Authentication required", request), HttpStatus.UNAUTHORIZED);
    }

    // ✅ Authenticated but not allowed to perform this action
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return new ResponseEntity<>(baseBody(HttpStatus.FORBIDDEN, "Access denied", request), HttpStatus.FORBIDDEN);
    }

    // ✅ Authenticated but trying to touch a resource that isn't theirs
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException ex, WebRequest request) {
        return new ResponseEntity<>(baseBody(HttpStatus.FORBIDDEN, ex.getMessage(), request), HttpStatus.FORBIDDEN);
    }

    // ✅ Rate limiting (OTP resend / verification attempts)
    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<Map<String, Object>> handleTooManyRequests(TooManyRequestsException ex, WebRequest request) {
        return new ResponseEntity<>(baseBody(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request), HttpStatus.TOO_MANY_REQUESTS);
    }

    // ✅ Refresh/session-token specific auth failures (expired, revoked, logged-out device, idle timeout)
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex, WebRequest request) {
        return new ResponseEntity<>(baseBody(HttpStatus.UNAUTHORIZED, ex.getMessage(), request), HttpStatus.UNAUTHORIZED);
    }

    // ✅ Resource genuinely doesn't exist
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        return new ResponseEntity<>(baseBody(HttpStatus.NOT_FOUND, ex.getMessage(), request), HttpStatus.NOT_FOUND);
    }

    // ✅ Business-logic errors thrown deliberately by services (e.g. "User not found", "Email already exists")
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex, WebRequest request) {
        return new ResponseEntity<>(baseBody(HttpStatus.BAD_REQUEST, ex.getMessage(), request), HttpStatus.BAD_REQUEST);
    }

    // ✅ Anything unexpected — never leak stack traces to the client
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex, WebRequest request) {
        return new ResponseEntity<>(baseBody(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", request), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private Map<String, Object> baseBody(HttpStatus status, String message, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));
        return body;
    }
}