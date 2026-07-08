package AgriTrackBackend.BOOKING;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "BOOKINGS")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BOOKING_ID")
    private Long bookingId;

    // the client (USERS.USER_ID, role CUSTOMER) who requested the service
    @Column(name = "CLIENT_ID", nullable = false)
    private Long clientId;

    @Column(name = "OWNER_ID", nullable = false)
    private Long ownerId;

    @Column(name = "TRACTOR_ID")
    private Long tractorId;

    // the driver assigned by the owner to fulfil this booking
    @Column(name = "DRIVER_ID")
    private Long driverId;

    @Column(name = "SERVICE_TYPE", length = 100)
    private String serviceType;

    @Column(name = "REQUESTED_DATE")
    private LocalDateTime requestedDate;

    @Column(name = "FIELD_SIZE", length = 50)
    private String fieldSize;

    @Column(name = "DURATION", length = 50)
    private String duration;

    @Column(name = "LOCATION", length = 255)
    private String location;

    @Column(name = "LATITUDE")
    private Double latitude;

    @Column(name = "LONGITUDE")
    private Double longitude;

    @Column(name = "AMOUNT", precision = 12, scale = 2)
    private BigDecimal amount;

    // PENDING / ACCEPTED / REJECTED / COMPLETED / CANCELLED
    @Column(name = "STATUS", length = 20)
    private String status;

    @Column(name = "NOTES", length = 500)
    private String notes;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
