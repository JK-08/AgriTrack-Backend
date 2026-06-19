package AgriTrackBackend.CUSTOMER;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "CUSTOMERS")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CUSTOMER_ID")
    private Long customerId;

    // ✅ The OWNER (USERS.USER_ID) who manages this customer
    @Column(name = "OWNER_ID", nullable = false)
    private Long ownerId;

    // ✅ Optional link to a USERS row (for marketplace clients who have a login)
    @Column(name = "USER_ID")
    private Long userId;

    @Column(name = "NAME", length = 150, nullable = false)
    private String name;

    @Column(name = "MOBILE_NO", length = 15)
    private String mobileNo;

    @Column(name = "EMAIL", length = 150)
    private String email;

    @Column(name = "ADDRESS", length = 255)
    private String address;

    @Column(name = "VILLAGE", length = 150)
    private String village;

    // NEW / EXISTING
    @Column(name = "CUSTOMER_TYPE", length = 20)
    private String customerType;

    @Column(name = "FARM_SIZE", length = 50)
    private String farmSize;

    @Column(name = "LATITUDE")
    private Double latitude;

    @Column(name = "LONGITUDE")
    private Double longitude;

    @Column(name = "PREFERRED_PAYMENT_METHOD", length = 30)
    private String preferredPaymentMethod;

    @Column(name = "PHOTO_URL", length = 500)
    private String photoUrl;

    @Column(name = "NOTES", length = 500)
    private String notes;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
