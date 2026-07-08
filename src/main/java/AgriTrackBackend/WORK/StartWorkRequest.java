package AgriTrackBackend.WORK;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartWorkRequest {
    private Long ownerId;
    private Long customerId;
    private Long tractorId;
    private Long driverId;
    private Long bookingId;
    private Long rateId;
    private String serviceType;
    private String notes;
}
