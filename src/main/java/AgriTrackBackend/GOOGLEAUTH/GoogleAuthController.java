package AgriTrackBackend.GOOGLEAUTH;

import AgriTrackBackend.SESSION.DeviceInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/google")
@CrossOrigin
@Tag(name = "Google Auth", description = "Sign in with Google ID token. Auto-registers a CUSTOMER account on first login.")
public class GoogleAuthController {

    @Autowired
    private GoogleAuthService service;

    @Operation(summary = "Login with Google", description = "No Authorization header required. Verifies the Google ID token, auto-registers the user if new, and issues an access+refresh token pair.")
    @PostMapping("/login")
    public Object googleLogin(
            @RequestBody GoogleLoginRequest request,
            HttpServletRequest httpRequest
    ) {

        try {

            return service.loginWithGoogle(
                    request.getIdToken(),
                    DeviceInfo.from(httpRequest),
                    DeviceInfo.clientIp(httpRequest)
            );

        } catch (Exception e) {

            return new ErrorResponse(
                    false,
                    "Invalid or Expired Google Token"
            );
        }
    }
}