package AgriTrackBackend.MPIN;

import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.SECURITY.JwtUtil;
import AgriTrackBackend.SESSION.AuthResponse;
import AgriTrackBackend.SESSION.DeviceInfo;
import AgriTrackBackend.SESSION.SessionService;
import AgriTrackBackend.USERS.LoginResponse;
import AgriTrackBackend.USERS.User;
import AgriTrackBackend.USERS.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class MpinService {

    @Autowired
    private MpinRepository mpinRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private AuditService auditService;

    // ✅ CREATE MPIN
    public String createMpin(String token, CreateMpinRequest request) {

        String email = jwtUtil.extractUsername(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (mpinRepository.findByUserUserId(user.getUserId()).isPresent()) {
            throw new RuntimeException("MPIN already exists");
        }

        Mpin mpin = new Mpin();

        mpin.setUser(user);
        mpin.setMpin(passwordEncoder.encode(request.getMpin()));

        mpinRepository.save(mpin);

        // never log the MPIN itself — metadata only
        auditService.log(AuditAction.CREATE, "Mpin", user.getUserId(), null, null);

        return "MPIN created successfully";
    }

    // ✅ UPDATE MPIN
    public String updateMpin(String token, UpdateMpinRequest request) {

        String email = jwtUtil.extractUsername(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Mpin mpin = mpinRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("MPIN not found"));

        if (!passwordEncoder.matches(request.getOldMpin(), mpin.getMpin())) {
            throw new RuntimeException("Old MPIN incorrect");
        }

        mpin.setMpin(passwordEncoder.encode(request.getNewMpin()));

        mpinRepository.save(mpin);

        auditService.log(AuditAction.UPDATE, "Mpin", user.getUserId(), null, null);

        return "MPIN updated successfully";
    }

    // ✅ RESET MPIN
    public String resetMpin(String token, ResetMpinRequest request) {

        String email = jwtUtil.extractUsername(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Password incorrect");
        }

        Mpin mpin = mpinRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("MPIN not found"));

        mpin.setMpin(passwordEncoder.encode(request.getNewMpin()));

        mpinRepository.save(mpin);

        auditService.log(AuditAction.MPIN_RESET, "Mpin", user.getUserId(), null, null);

        return "MPIN reset successfully";
    }

    // ✅ DELETE MPIN
    public String deleteMpin(String token) {

        String email = jwtUtil.extractUsername(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Mpin mpin = mpinRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("MPIN not found"));

        mpinRepository.delete(mpin);

        auditService.log(AuditAction.DELETE, "Mpin", user.getUserId(), null, null);

        return "MPIN deleted successfully";
    }

    // ✅ LOGIN WITH MPIN
    public LoginResponse loginWithMpin(LoginMpinRequest request, DeviceInfo device, String ip) {

        Mpin mpin = mpinRepository.findByUserMobileNo(request.getMobileNo())
                .orElseThrow(() -> new RuntimeException("MPIN not found"));

        if (!passwordEncoder.matches(request.getMpin(), mpin.getMpin())) {
            sessionService.recordFailedLogin(mpin.getUser().getUserId(), ip, device, "Invalid MPIN");
            throw new RuntimeException("Invalid MPIN");
        }

        User user = mpin.getUser();

        AuthResponse auth = sessionService.createSession(user, device, ip);

        return new LoginResponse(
                auth.getAccessToken(),
                auth.getRole(),
                auth.getName(),
                auth.getUserId(),
                auth.getRefreshToken(),
                auth.getExpiresIn(),
                auth.getSessionId()
        );
    }
}