package AgriTrackBackend.SECURITY;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    // secret is externalized to application.properties (jwt.secret) instead
    // of being hardcoded, so it can differ per environment / be rotated
    // without a code change.
    @Value("${jwt.secret}")
    private String secret;

    // short-lived access token — the client refreshes it automatically via
    // POST /api/v1/auth/refresh, so the user never has to log in again just
    // because this expired.
    @Value("${jwt.access-expiration-ms:900000}")
    private long accessExpirationMs;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private Key key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public long getAccessExpirationMs() {
        return accessExpirationMs;
    }

    /** Access token — same signature as before so every existing call site keeps working. */
    public String generateToken(Long userId, String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("role", role)
                .claim("type", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public Long extractUserId(String token) {
        Object raw = getClaims(token).get("userId");
        if (raw == null) return null;
        return Long.valueOf(raw.toString());
    }

    public boolean validate(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // ------------------------------------------------------------------
    // Refresh tokens: opaque, high-entropy, random strings — deliberately
    // NOT JWTs. They are meaningless outside this backend, so there is
    // nothing to decode/replay if intercepted; the server is the only
    // place that can map one back to a session, and only via its hash.
    // ------------------------------------------------------------------

    public String generateRefreshToken() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 hash of a refresh token, stored in place of the raw value (never store it in plaintext). */
    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
