package AgriTrackBackend.MPIN;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetMpinRequest {

    private String password;
    private String newMpin;
}