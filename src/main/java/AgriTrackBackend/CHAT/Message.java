package AgriTrackBackend.CHAT;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "MESSAGES")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MESSAGE_ID")
    private Long messageId;

    @Column(name = "CHAT_ID", nullable = false)
    private Long chatId;

    @Column(name = "SENDER_ID", nullable = false)
    private Long senderId;

    @Column(name = "MESSAGE_TEXT", length = 1000)
    private String messageText;

    @Column(name = "IS_READ")
    private Boolean isRead = false;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
