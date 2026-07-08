package AgriTrackBackend.CONFIG;

import AgriTrackBackend.SECURITY.CustomAuthenticationEntryPoint;
import AgriTrackBackend.SECURITY.JwtFilter;
import AgriTrackBackend.SECURITY.RateLimitFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * NOTE ON SCOPE (read before changing this again):
 * This chain requires a valid JWT for every endpoint except the public
 * auth/onboarding routes listed below. Ownership-level authorization
 * (verifying an ownerId/customerId/driverId in a request actually belongs
 * to the caller) is enforced per-controller/service via CurrentUser, not
 * here. Refresh-token endpoints (/auth/refresh, /auth/logout) are public
 * at this layer because they authenticate via the refresh token in the
 * request body/session table, not via the Authorization header.
 */
@Configuration
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Autowired
    private CustomAuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .exceptionHandling(e -> e.authenticationEntryPoint(authenticationEntryPoint))

                .authorizeHttpRequests(auth -> auth

                        // ✅ CORS preflight must always be allowed through
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ✅ PUBLIC APIs — no login required
                        .requestMatchers(
                                "/api/v1/health",
                                "/api/v1/user/register",
                                "/api/v1/user/login",
                                "/api/v1/google/login",
                                "/api/v1/mpin/login",
                                "/api/v1/onboarding/getAll",
                                "/api/v1/onboarding/getById/**",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout",
                                "/api/v1/user/forgot-password",
                                "/api/v1/user/verify-otp",
                                "/api/v1/user/reset-password",
                                "/api/v1/mpin/forgot/send-otp",
                                "/api/v1/mpin/forgot/verify",
                                "/api/v1/mpin/forgot/reset",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml"
                        ).permitAll()

                        // 🔒 EVERYTHING ELSE NEEDS A VALID TOKEN
                        .anyRequest().authenticated()
                )

                // rate limiting runs first (before auth is even parsed), then JWT auth
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, JwtFilter.class);

        return http.build();
    }
}
