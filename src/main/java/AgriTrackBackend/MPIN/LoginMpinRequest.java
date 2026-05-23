package AgriTrackBackend.MPIN;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginMpinRequest {

    private String mobileNo;
    private String mpin;
}