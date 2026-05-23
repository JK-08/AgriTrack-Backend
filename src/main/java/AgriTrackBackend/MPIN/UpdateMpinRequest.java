package AgriTrackBackend.MPIN;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMpinRequest {

    private String oldMpin;
    private String newMpin;
}