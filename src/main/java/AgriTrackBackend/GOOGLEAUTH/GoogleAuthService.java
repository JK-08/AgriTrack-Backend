package AgriTrackBackend.GOOGLEAUTH;

import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.SECURITY.JwtUtil;
import AgriTrackBackend.SESSION.AuthResponse;
import AgriTrackBackend.SESSION.DeviceInfo;
import AgriTrackBackend.SESSION.SessionService;
import AgriTrackBackend.USERS.Role;
import AgriTrackBackend.USERS.User;
import AgriTrackBackend.USERS.UserRepository;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class GoogleAuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private AuditService auditService;

    // externalized — was hardcoded; see GOOGLE_CLIENT_ID in .env.example
    @Value("${google.client-id}")
    private String CLIENT_ID;

    public GoogleLoginResponse loginWithGoogle(
            String idTokenString, DeviceInfo device, String ip
    ) throws Exception {

        GoogleIdTokenVerifier verifier =
                new GoogleIdTokenVerifier.Builder(
                        new NetHttpTransport(),
                        GsonFactory.getDefaultInstance()
                )
                        .setAudience(
                                Collections.singletonList(
                                        CLIENT_ID
                                )
                        )
                        .build();

        GoogleIdToken idToken =
                verifier.verify(idTokenString);

        if (idToken == null) {

            throw new RuntimeException(
                    "Invalid Google Token"
            );
        }

        GoogleIdToken.Payload payload =
                idToken.getPayload();

        String email = payload.getEmail();

        String name =
                (String) payload.get("name");

        User user =
                userRepository.findByEmail(email)
                        .orElse(null);

        // AUTO REGISTER
        if (user == null) {

            user = new User();

            user.setName(name);

            user.setEmail(email);

            user.setMobileNo("GOOGLE_USER");

            user.setPassword("GOOGLE_LOGIN");

            user.setRole(Role.CUSTOMER);

            user = userRepository.save(user);
            // never log password/credential fields — metadata only
            auditService.log(AuditAction.CREATE, "User", user.getUserId(), null,
                    java.util.Map.of("email", user.getEmail(), "source", "GOOGLE_AUTO_REGISTER"));
        }

        AuthResponse auth = sessionService.createSession(user, device, ip);

        return new GoogleLoginResponse(

                true,

                "Google Login Success",

                auth.getAccessToken(),

                user.getUserId(),

                user.getName(),

                user.getEmail(),

                user.getRole().name(),

                auth.getRefreshToken(),

                auth.getExpiresIn(),

                auth.getSessionId()
        );
    }
}