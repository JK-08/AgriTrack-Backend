package AgriTrackBackend.GOOGLEAUTH;

import AgriTrackBackend.SECURITY.JwtUtil;
import AgriTrackBackend.USERS.Role;
import AgriTrackBackend.USERS.User;
import AgriTrackBackend.USERS.UserRepository;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class GoogleAuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // ✅ Replace with your GOOGLE WEB CLIENT ID
    private static final String CLIENT_ID =
            "YOUR_GOOGLE_WEB_CLIENT_ID";

    public Object loginWithGoogle(String idTokenString) throws Exception {

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        )
                .setAudience(Collections.singletonList(CLIENT_ID))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);

        if (idToken == null) {
            throw new RuntimeException("Invalid Google Token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        String email = payload.getEmail();
        String name = (String) payload.get("name");

        User user = userRepository.findByEmail(email).orElse(null);

        // ✅ Auto register if user not exists
        if (user == null) {

            user = new User();
            user.setName(name);
            user.setEmail(email);

            // dummy mobile
            user.setMobileNo("GOOGLE_USER");

            // no password
            user.setPassword("GOOGLE_LOGIN");

            // default role
            user.setRole(Role.CUSTOMER);

            user = userRepository.save(user);
        }

        String jwtToken = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().name()
        );

        return new AgriTrackBackend.USERS.LoginResponse(
                jwtToken,
                user.getRole().name(),
                user.getName(),
                user.getUserId()
        );
    }
}