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

    private static final String CLIENT_ID =
            "740018456459-c47e4vmglnkvjvjpemcrhvkfk1v09618.apps.googleusercontent.com";

    public GoogleLoginResponse loginWithGoogle(
            String idTokenString
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
        }

        String jwtToken =
                jwtUtil.generateToken(
                        user.getEmail(),
                        user.getRole().name()
                );

        return new GoogleLoginResponse(

                true,

                "Google Login Success",

                jwtToken,

                user.getUserId(),

                user.getName(),

                user.getEmail(),

                user.getRole().name()
        );
    }
}