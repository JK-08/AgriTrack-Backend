package AgriTrackBackend.USERS;

import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.SECURITY.JwtUtil;
import AgriTrackBackend.SESSION.DeviceInfo;
import AgriTrackBackend.SESSION.AuthResponse;
import AgriTrackBackend.SESSION.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository repository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private AuditService auditService;

    // ✅ REGISTER
    // ✅ REGISTER
    public User register(User user) {

        boolean noEmail = user.getEmail() == null || user.getEmail().isBlank();
        boolean noMobile = user.getMobileNo() == null || user.getMobileNo().isBlank();
        if (noEmail && noMobile) {
            throw new RuntimeException("Email or mobile number is required");
        }

        // CHECK EMAIL
        if (!noEmail && repository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        // CHECK MOBILE
        if (!noMobile && repository.findByMobileNo(user.getMobileNo()).isPresent()) {
            throw new RuntimeException("Mobile number already exists");
        }

        // 🔐 Encrypt password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User saved = repository.save(user);
        // never write the password hash to the audit trail
        auditService.log(AuditAction.CREATE, "User", saved.getUserId(), null,
                Map.of("userId", saved.getUserId(), "name", saved.getName(), "role", saved.getRole()));
        return saved;
    }

    // ✅ LOGIN — now issues an access+refresh token pair and creates a
    // per-device session (enterprise auth: multi-device login, refresh
    // rotation, logout-all, login history). Existing response fields are
    // unchanged; refreshToken/expiresIn/sessionId are additive.
    public LoginResponse login(String username, String password, String role, DeviceInfo device, String ip) {

        Optional<User> maybeUser = repository.findByEmailOrMobileNo(username, username);

        if (maybeUser.isEmpty()) {
            sessionService.recordFailedLogin(null, ip, device, "User not found");
            throw new RuntimeException("User not found");
        }

        User user = maybeUser.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            sessionService.recordFailedLogin(user.getUserId(), ip, device, "Invalid password");
            throw new RuntimeException("Invalid password");
        }

        if (!user.getRole().name().equalsIgnoreCase(role)) {
            sessionService.recordFailedLogin(user.getUserId(), ip, device, "Invalid role access");
            throw new RuntimeException("Invalid role access");
        }

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

    // ✅ GET ALL
    public List<User> getAll() {
        return repository.findAll();
    }

    // ✅ GET BY ID
    public User getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ✅ DELETE
    public void deleteById(Long id) {
        repository.deleteById(id);
        auditService.log(AuditAction.DELETE, "User", id, null, null);
    }
}