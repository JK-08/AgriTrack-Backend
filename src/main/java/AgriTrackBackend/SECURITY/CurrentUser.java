package AgriTrackBackend.SECURITY;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Reusable accessor for "who is making this request" — backed by the JWT
 * that {@link JwtFilter} already validated. Every service that reads,
 * updates or deletes an owner/customer/driver-scoped resource should use
 * this instead of trusting an id that came from the client.
 */
public final class CurrentUser {

    private CurrentUser() {}

    public static AuthenticatedUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new IllegalStateException("No authenticated user in context");
        }
        return user;
    }

    public static Long id() {
        return get().userId();
    }

    public static String role() {
        return get().role();
    }

    public static boolean isRole(String expected) {
        return expected != null && expected.equalsIgnoreCase(role());
    }

    /** Throws ForbiddenException unless the current user's id equals the given id. */
    public static void requireSelf(Long id) {
        if (id == null || !id.equals(id())) {
            throw new AgriTrackBackend.EXCEPTION.ForbiddenException("You do not have access to this resource");
        }
    }
}
