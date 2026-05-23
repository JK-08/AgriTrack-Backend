package AgriTrackBackend.MPIN;

import AgriTrackBackend.SECURITY.JwtUtil;
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

        return "MPIN deleted successfully";
    }

    // ✅ LOGIN WITH MPIN
    public LoginResponse loginWithMpin(LoginMpinRequest request) {

        Mpin mpin = mpinRepository.findByUserMobileNo(request.getMobileNo())
                .orElseThrow(() -> new RuntimeException("MPIN not found"));

        if (!passwordEncoder.matches(request.getMpin(), mpin.getMpin())) {
            throw new RuntimeException("Invalid MPIN");
        }

        User user = mpin.getUser();

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
}