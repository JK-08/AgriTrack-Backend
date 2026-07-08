package AgriTrackBackend.DRIVER;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "DRIVER_LOCATIONS")
public class DriverLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LOCATION_ID")
    private Long locationId;

    @Column(name = "DRIVER_ID", nullable = false)
    private Long driverId;

    @Column(name = "BOOKING_ID")
    private Long bookingId;

    @Column(name = "WORK_ID")
    private Long workId;

    @Column(name = "LATITUDE", nullable = false)
    private Double latitude;

    @Column(name = "LONGITUDE", nullable = false)
    private Double longitude;

    @Column(name = "RECORDED_AT", insertable = false, updatable = false)
    private LocalDateTime recordedAt;
}
