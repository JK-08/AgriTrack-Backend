package AgriTrackBackend.USERS;

import AgriTrackBackend.SECURITY.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository repository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // ✅ REGISTER
    // ✅ REGISTER
    public User register(User user) {

        // CHECK EMAIL
        if (repository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        // CHECK MOBILE
        if (repository.findByMobileNo(user.getMobileNo()).isPresent()) {
            throw new RuntimeException("Mobile number already exists");
        }

        // 🔐 Encrypt password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return repository.save(user);
    }

    // ✅ LOGIN (FINAL VERSION)
    // ✅ LOGIN ROLE BASED
    public LoginResponse login(String username, String password, String role) {

        User user = repository.findByEmailOrMobileNo(username, username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ✅ password check
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // ✅ role check
        if (!user.getRole().name().equalsIgnoreCase(role)) {
            throw new RuntimeException("Invalid role access");
        }

        // ✅ generate token
        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().name()
        );

        return new LoginResponse(
                token,
                user.getRole().name(),
                user.getName(),
                user.getUserId()
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
    }
}