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

            InputStream serviceAccount =
                    getClass().getClassLoader()
                            .getResourceAsStream("firebase/agritrack-8ef35-58baa73294ae.json");

            if (serviceAccount == null) {
                throw new RuntimeException("Firebase JSON file not found");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            System.out.println("✅ Firebase Initialized");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Firebase Initialization Failed");
        }
    }
}