package AgriTrackBackend.GOOGLEAUTH;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/google")
@CrossOrigin
public class GoogleAuthController {

    @Autowired
    private GoogleAuthService service;

    @PostMapping("/login")
    public Object googleLogin(
            @RequestBody GoogleLoginRequest request
    ) {

        try {

            return service.loginWithGoogle(
                    request.getIdToken()
            );

        } catch (Exception e) {

            return new ErrorResponse(
                    false,
                    "Invalid or Expired Google Token"
            );
        }
    }
}