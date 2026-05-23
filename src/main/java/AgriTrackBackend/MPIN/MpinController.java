package AgriTrackBackend.MPIN;

import AgriTrackBackend.USERS.LoginResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mpin")
@CrossOrigin
public class MpinController {

    @Autowired
    private MpinService service;

    // ✅ CREATE
    @PostMapping("/create")
    public String createMpin(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreateMpinRequest request
    ) {

        String token = authHeader.substring(7);

        return service.createMpin(token, request);
    }

    // ✅ UPDATE
    @PutMapping("/update")
    public String updateMpin(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UpdateMpinRequest request
    ) {

        String token = authHeader.substring(7);

        return service.updateMpin(token, request);
    }

    // ✅ RESET
    @PutMapping("/reset")
    public String resetMpin(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ResetMpinRequest request
    ) {

        String token = authHeader.substring(7);

        return service.resetMpin(token, request);
    }

    // ✅ DELETE
    @DeleteMapping("/delete")
    public String deleteMpin(
            @RequestHeader("Authorization") String authHeader
    ) {

        String token = authHeader.substring(7);

        return service.deleteMpin(token);
    }

    // ✅ LOGIN WITH MPIN
    @PostMapping("/login")
    public LoginResponse loginWithMpin(
            @RequestBody LoginMpinRequest request
    ) {
        return service.loginWithMpin(request);
    }
}