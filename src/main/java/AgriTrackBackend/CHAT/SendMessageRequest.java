package AgriTrackBackend.CHAT;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMessageRequest {
    private Long ownerId;
    private Long clientId;
    private Long senderId;
    private String messageText;
}
