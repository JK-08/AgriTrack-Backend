package AgriTrackBackend.GOOGLEAUTH;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ErrorResponse {

    private Boolean success;

    private String message;
}