package AgriTrackBackend.USERS;

import AgriTrackBackend.OTP.OtpPurpose;
import AgriTrackBackend.OTP.OtpResponse;
import AgriTrackBackend.OTP.OtpService;
import AgriTrackBackend.OTP.ResetPasswordRequest;
import AgriTrackBackend.OTP.SendOtpRequest;
import AgriTrackBackend.OTP.VerifyOtpRequest;
import AgriTrackBackend.OTP.VerifyOtpResponse;
import AgriTrackBackend.SESSION.DeviceInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
@CrossOrigin
@Tag(name = "Users / Auth", description = "Registration, login, and forgot-password (OTP) recovery")
public class UserController {

    @Autowired
    private UserService service;

    @Autowired
    private OtpService otpService;

    @Operation(summary = "Register a new user", description = "Role is OWNER, DRIVER, or CUSTOMER. Password is bcrypt-hashed before storage.")
    @PostMapping("/register")
    public User register(@Valid @RequestBody User user) {
        return service.register(user);
    }

    @Operation(
            summary = "Login",
            description = "Returns a short-lived (15 min) access token plus a refresh token. "
                    + "Call POST /api/v1/auth/refresh to get a new access token before/when this one expires.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(
                            examples = @ExampleObject(value = "{\"token\":\"eyJhbGciOi...\",\"role\":\"OWNER\","
                                    + "\"name\":\"Ravi Kumar\",\"userId\":12,\"refreshToken\":\"Z3f8...\","
                                    + "\"expiresIn\":900,\"sessionId\":45}"))),
                    @ApiResponse(responseCode = "400", description = "User not found / invalid password / role mismatch"),
                    @ApiResponse(responseCode = "429", description = "Too many login attempts from this IP — retry later")
            }
    )
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {

        return service.login(
                request.getUsername(),
                request.getPassword(),
                request.getRole(),
                DeviceInfo.from(httpRequest),
                DeviceInfo.clientIp(httpRequest)
        );
    }


    // ✅ GET ALL
    @GetMapping("/getall")
    public List<User> getAll() {
        return service.getAll();
    }

    // ✅ GET BY ID
    @GetMapping("/getbyid/{id}")
    public User getById(@PathVariable Long id) {
        return service.getById(id);
    }

    // ✅ DELETE
    @DeleteMapping("/deletebyid/{id}")
    public String delete(@PathVariable Long id) {
        service.deleteById(id);
        return "User deleted successfully";
    }

    // ------------------------------------------------------------------
    // FORGOT PASSWORD — 3-step OTP recovery (public, no JWT required)
    // ------------------------------------------------------------------

    // ✅ STEP 1 — request an OTP for the account's email/mobile
    @PostMapping("/forgot-password")
    public OtpResponse forgotPassword(@Valid @RequestBody SendOtpRequest request) {
        return otpService.sendOtp(request.getIdentifier(), OtpPurpose.PASSWORD_RESET);
    }

    // ✅ STEP 2 — verify the OTP, receive a short-lived reset token
    @PostMapping("/verify-otp")
    public VerifyOtpResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return otpService.verifyOtp(request.getIdentifier(), OtpPurpose.PASSWORD_RESET, request.getOtp());
    }

    // ✅ STEP 3 — spend the reset token to actually set the new password
    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        otpService.resetPassword(request.getIdentifier(), request.getResetToken(), request.getNewPassword());
        return Map.of("message", "Password reset successfully");
    }
}