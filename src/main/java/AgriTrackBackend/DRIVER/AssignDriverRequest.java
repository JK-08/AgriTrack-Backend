package AgriTrackBackend.DRIVER;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignDriverRequest {
    private Long ownerId;
    private Long tractorId;
    private Long driverId;
}
