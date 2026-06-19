package AgriTrackBackend.WORK;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartWorkRequest {
    private Long ownerId;
    private Long customerId;
    private Long tractorId;
    private Long rateId;
    private String serviceType;
    private String notes;
}
