package AgriTrackBackend.SESSION;

import AgriTrackBackend.COMMON.PageResponse;
import AgriTrackBackend.SECURITY.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Session / refresh-token lifecycle endpoints. Login itself stays on the
 * existing role-specific controllers (/user/login, /mpin/login,
 * /google/login) to preserve backward compatibility — they all now issue
 * sessions through {@link SessionService} under the hood.
 */
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin
@Tag(name = "Auth / Sessions", description = "Refresh-token rotation, logout, device sessions, login history")
public class AuthController {

    @Autowired
    private SessionService sessionService;

    @Operation(
            summary = "Rotate refresh token",
            description = "Exchanges a still-valid refresh token for a new access token + a new refresh token "
                    + "(the old refresh token is revoked immediately — rotation). Call this automatically "
                    + "whenever an API call returns 401 with an expired access token. No Authorization header required.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "New token pair issued", content = @Content(
                            examples = @ExampleObject(value = "{\"accessToken\":\"eyJhbGciOi...\",\"refreshToken\":\"Z3f8...\","
                                    + "\"tokenType\":\"Bearer\",\"expiresIn\":900,\"role\":\"OWNER\",\"name\":\"Ravi Kumar\","
                                    + "\"userId\":12,\"sessionId\":45}"))),
                    @ApiResponse(responseCode = "401", description = "Refresh token invalid, expired, revoked, or session idle-timed-out")
            }
    )
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest httpRequest) {
        return sessionService.refresh(
                request.getRefreshToken(),
                DeviceInfo.from(httpRequest),
                DeviceInfo.clientIp(httpRequest)
        );
    }

    @Operation(summary = "Log out this device", description = "Revokes the given refresh token. Idempotent — "
            + "an already-revoked or unknown token still returns success. No Authorization header required.")
    @PostMapping("/logout")
    public Map<String, String> logout(@Valid @RequestBody LogoutRequest request) {
        sessionService.logout(request.getRefreshToken());
        return Map.of("message", "Logged out successfully");
    }

    @Operation(summary = "Log out every device", description = "Revokes every active session for the authenticated user.")
    @PostMapping("/logout-all")
    public Map<String, String> logoutAll() {
        sessionService.logoutAll(CurrentUser.id());
        return Map.of("message", "Logged out from all devices");
    }

    @Operation(summary = "List my devices/sessions", description = "Active and past sessions for the authenticated user.")
    @GetMapping("/sessions")
    public List<SessionDto> sessions() {
        return sessionService.listSessions(CurrentUser.id());
    }

    @Operation(summary = "Revoke one session by id", description = "Must belong to the authenticated user (403 otherwise).")
    @DeleteMapping("/session/{id}")
    public Map<String, String> deleteSession(@PathVariable Long id) {
        sessionService.revokeSession(CurrentUser.id(), id);
        return Map.of("message", "Session revoked");
    }

    @Operation(summary = "Login history", description = "Successful and failed login attempts for the authenticated user.")
    @GetMapping("/login-history")
    public List<LoginHistory> loginHistory() {
        return sessionService.loginHistory(CurrentUser.id());
    }

    @Operation(summary = "Paged/search/sort/filter my devices/sessions",
            description = "Additive alongside /sessions. Filter by revoked and/or platform.")
    @GetMapping("/sessions-paged")
    public PageResponse<SessionDto> sessionsPaged(
            @RequestParam(required = false) Boolean revoked,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        return sessionService.searchSessionsPaged(CurrentUser.id(), revoked, platform, page, size, sortBy, sortDir);
    }

    @Operation(summary = "Paged/search/sort/filter login history",
            description = "Additive alongside /login-history. Filter by status (SUCCESS/FAILURE) and/or platform.")
    @GetMapping("/login-history-paged")
    public PageResponse<LoginHistory> loginHistoryPaged(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        return sessionService.searchLoginHistoryPaged(CurrentUser.id(), status, platform, page, size, sortBy, sortDir);
    }
}
