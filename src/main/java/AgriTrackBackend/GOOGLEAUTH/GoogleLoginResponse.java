package AgriTrackBackend.GOOGLEAUTH;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GoogleLoginResponse {

    private Boolean success;

    private String message;

    private String token;

    private Long userId;

    private String name;

    private String email;

    private String role;
}