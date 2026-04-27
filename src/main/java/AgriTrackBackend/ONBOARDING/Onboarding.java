package AgriTrackBackend.ONBOARDING;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "ONBOARDINGS")
public class Onboarding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ONBOARDING_ID")
    private Long onboardingId;

    @Column(name = "TITLE", length = 150, nullable = false)
    private String title;

    @Column(name = "SUBTITLE", length = 255)
    private String subtitle;

    @Column(name = "IMAGE_URL", length = 500)
    private String imageUrl;

    // ✅ IMPORTANT FIX
    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}