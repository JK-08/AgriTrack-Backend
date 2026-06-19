package AgriTrackBackend.RATING;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "RATINGS")
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RATING_ID")
    private Long ratingId;

    @Column(name = "BOOKING_ID")
    private Long bookingId;

    @Column(name = "CLIENT_ID", nullable = false)
    private Long clientId;

    @Column(name = "OWNER_ID", nullable = false)
    private Long ownerId;

    // 1 - 5
    @Column(name = "RATING_VALUE")
    private Integer ratingValue;

    @Column(name = "REVIEW", length = 1000)
    private String review;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
