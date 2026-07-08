package AgriTrackBackend.SECURITY;

/**
 * The principal placed into the Spring Security context by {@link JwtFilter}
 * for every authenticated request. Carries the caller's real USER_ID (never
 * trust an ownerId/customerId/driverId that arrives in a request path/body —
 * always compare it against this).
 */
public record AuthenticatedUser(Long userId, String username, String role) {
}
