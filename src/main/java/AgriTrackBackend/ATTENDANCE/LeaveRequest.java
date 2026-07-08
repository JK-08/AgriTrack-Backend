package AgriTrackBackend.ATTENDANCE;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class LeaveRequest {

    @NotNull(message = "driverId is required")
    private Long driverId;

    @NotNull(message = "date is required")
    private LocalDate date;

    private String reason;
}
