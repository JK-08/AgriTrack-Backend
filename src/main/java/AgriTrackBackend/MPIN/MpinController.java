package AgriTrackBackend.MPIN;

import AgriTrackBackend.OTP.OtpPurpose;
import AgriTrackBackend.OTP.OtpResponse;
import AgriTrackBackend.OTP.OtpService;
import AgriTrackBackend.OTP.ResetMpinViaOtpRequest;
import AgriTrackBackend.OTP.SendOtpRequest;
import AgriTrackBackend.OTP.VerifyOtpRequest;
import AgriTrackBackend.OTP.VerifyOtpResponse;
import AgriTrackBackend.SESSION.DeviceInfo;
import AgriTrackBackend.USERS.LoginResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/mpin")
@CrossOrigin
@Tag(name = "MPIN", description = "4-6 digit quick-login PIN, plus OTP-based forgot-MPIN recovery. The MPIN value itself is never returned or logged.")
public class MpinController {

    @Autowired
    private MpinService service;

    @Autowired
    private OtpService otpService;

    // ✅ CREATE
    @Operation(summary = "Create an MPIN", description = "Requires a valid Bearer token. Fails if an MPIN already exists for this user.")
    @PostMapping("/create")
    public String createMpin(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreateMpinRequest request
    ) {

        String token = authHeader.substring(7);

        return service.createMpin(token, request);
    }

    // ✅ UPDATE
    @Operation(summary = "Update MPIN", description = "Requires the current MPIN.")
    @PutMapping("/update")
    public String updateMpin(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UpdateMpinRequest request
    ) {

        String token = authHeader.substring(7);

        return service.updateMpin(token, request);
    }

    // ✅ RESET
    @Operation(summary = "Reset MPIN", description = "Requires the account password (not the old MPIN). Audited as MPIN_RESET.")
    @PutMapping("/reset")
    public String resetMpin(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ResetMpinRequest request
    ) {

        String token = authHeader.substring(7);

        return service.resetMpin(token, request);
    }

    // ✅ DELETE
    @Operation(summary = "Delete MPIN")
    @DeleteMapping("/delete")
    public String deleteMpin(
            @RequestHeader("Authorization") String authHeader
    ) {

        String token = authHeader.substring(7);

        return service.deleteMpin(token);
    }

    // ✅ LOGIN WITH MPIN
    @Operation(summary = "Login with MPIN", description = "No Authorization header required. Issues an access+refresh token pair on success; audits LOGIN/LOGIN_FAILED.")
    @PostMapping("/login")
    public LoginResponse loginWithMpin(
            @RequestBody LoginMpinRequest request,
            HttpServletRequest httpRequest
    ) {
        return service.loginWithMpin(request, DeviceInfo.from(httpRequest), DeviceInfo.clientIp(httpRequest));
    }

    // ------------------------------------------------------------------
    // FORGOT MPIN — 3-step OTP recovery (public, no JWT required).
    // Distinct from /reset above, which requires the user to know their
    // current password and already be authenticated.
    // ------------------------------------------------------------------

    // ✅ STEP 1
    @Operation(summary = "Forgot MPIN — send OTP", description = "Public, no JWT required. In DEV_OTP_MODE, the OTP is returned in the response instead of being sent via SMS/email.")
    @PostMapping("/forgot/send-otp")
    public OtpResponse forgotSendOtp(@Valid @RequestBody SendOtpRequest request) {
        return otpService.sendOtp(request.getIdentifier(), OtpPurpose.MPIN_RESET);
    }

    // ✅ STEP 2
    @Operation(summary = "Forgot MPIN — verify OTP", description = "Returns a one-time reset token to be used in step 3. Rate-limited verification attempts.")
    @PostMapping("/forgot/verify")
    public VerifyOtpResponse forgotVerify(@Valid @RequestBody VerifyOtpRequest request) {
        return otpService.verifyOtp(request.getIdentifier(), OtpPurpose.MPIN_RESET, request.getOtp());
    }

    // ✅ STEP 3
    @Operation(summary = "Forgot MPIN — reset with token", description = "Consumes the one-time reset token from step 2. Audited as MPIN_RESET.")
    @PostMapping("/forgot/reset")
    public Map<String, String> forgotReset(@Valid @RequestBody ResetMpinViaOtpRequest request) {
        otpService.resetMpin(request.getIdentifier(), request.getResetToken(), request.getNewMpin());
        return Map.of("message", "MPIN reset successfully");
    }
}