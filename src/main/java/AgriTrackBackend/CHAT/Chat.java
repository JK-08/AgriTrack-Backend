package AgriTrackBackend.CHAT;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "CHATS")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CHAT_ID")
    private Long chatId;

    @Column(name = "OWNER_ID", nullable = false)
    private Long ownerId;

    @Column(name = "CLIENT_ID", nullable = false)
    private Long clientId;

    @Column(name = "LAST_MESSAGE", length = 500)
    private String lastMessage;

    @Column(name = "LAST_MESSAGE_TIME")
    private LocalDateTime lastMessageTime;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
