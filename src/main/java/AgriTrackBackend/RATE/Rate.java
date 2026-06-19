package AgriTrackBackend.RATE;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "RATES")
public class Rate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RATE_ID")
    private Long rateId;

    @Column(name = "OWNER_ID", nullable = false)
    private Long ownerId;

    // PLOUGHING / HARVESTING / ROTAVATOR ...
    @Column(name = "SERVICE_TYPE", length = 100, nullable = false)
    private String serviceType;

    @Column(name = "MACHINE_TYPE", length = 50)
    private String machineType;

    @Column(name = "PRICE_PER_MINUTE", precision = 10, scale = 2)
    private BigDecimal pricePerMinute;

    @Column(name = "PRICE_PER_TEN_MINUTES", precision = 10, scale = 2)
    private BigDecimal pricePerTenMinutes;

    @Column(name = "PRICE_PER_HOUR", precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    @Column(name = "IS_ACTIVE")
    private Boolean isActive = true;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
