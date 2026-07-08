package AgriTrackBackend.SECURITY;

import AgriTrackBackend.SESSION.DeviceInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global rate limiting, applied before authentication runs (registered
 * ahead of JwtFilter). Sensitive/abuse-prone endpoints — login, OTP
 * send/verify, token refresh — get tight per-IP limits; everything else
 * under /api/v1/** gets a much looser default limit as a basic DoS
 * backstop. Limits are per-instance (see RateLimiter) — fine for the
 * current single-instance deployment.
 */
@Component
public class RateLimitFilter implements Filter {

    private final RateLimiter limiter = new RateLimiter();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Value("${ratelimit.login.per-minute:5}")
    private int loginPerMinute;

    @Value("${ratelimit.otp.per-minute:5}")
    private int otpPerMinute;

    @Value("${ratelimit.refresh.per-minute:10}")
    private int refreshPerMinute;

    @Value("${ratelimit.default.per-minute:120}")
    private int defaultPerMinute;

    private static final long WINDOW_MS = 60_000L;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI();
        if (!path.startsWith("/api/v1/")) {
            chain.doFilter(request, response);
            return;
        }

        String ip = DeviceInfo.clientIp(req);
        RateLimit rule = classify(path);
        String key = ip + ":" + rule.category;

        if (!limiter.tryConsume(key, rule.limit, WINDOW_MS)) {
            writeTooManyRequests(res, rule.category);
            return;
        }

        chain.doFilter(request, response);
    }

    private RateLimit classify(String path) {
        if (path.equals("/api/v1/user/login")
                || path.equals("/api/v1/mpin/login")
                || path.equals("/api/v1/google/login")) {
            return new RateLimit("login", loginPerMinute);
        }
        if (path.contains("/forgot-password") || path.contains("/verify-otp")
                || path.contains("/forgot/send-otp") || path.contains("/forgot/verify")
                || path.contains("/forgot/reset") || path.equals("/api/v1/user/reset-password")) {
            return new RateLimit("otp", otpPerMinute);
        }
        if (path.equals("/api/v1/auth/refresh")) {
            return new RateLimit("refresh", refreshPerMinute);
        }
        return new RateLimit("default", defaultPerMinute);
    }

    private void writeTooManyRequests(HttpServletResponse res, String category) throws IOException {
        res.setStatus(429); // HttpServletResponse has no named constant for 429
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", 429);
        body.put("error", "Too Many Requests");
        body.put("message", "Rate limit exceeded for " + category + ". Please try again later.");

        res.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private record RateLimit(String category, int limit) {}
}
