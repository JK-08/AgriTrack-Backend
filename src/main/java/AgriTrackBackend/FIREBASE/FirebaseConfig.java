package AgriTrackBackend.FIREBASE;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {

        try {

            // ✅ Read firebase-service-account.json
            InputStream serviceAccount =
                    getClass()
                            .getClassLoader()
                            .getResourceAsStream(
                                    "firebase-service-account.json"
                            );

            FirebaseOptions options =
                    FirebaseOptions.builder()
                            .setCredentials(
                                    GoogleCredentials
                                            .fromStream(serviceAccount)
                            )
                            .build();

            // ✅ Initialize Firebase
            if (FirebaseApp.getApps().isEmpty()) {

                FirebaseApp.initializeApp(options);

                System.out.println(
                        "✅ Firebase Initialized Successfully"
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            System.out.println(
                    "❌ Firebase Initialization Failed"
            );
        }
    }
}