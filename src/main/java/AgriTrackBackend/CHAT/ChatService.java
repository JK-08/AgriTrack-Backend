package AgriTrackBackend.CHAT;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private MessageRepository messageRepository;

    // ✅ Find existing chat or create a new one
    @Transactional
    public Chat getOrCreateChat(Long ownerId, Long clientId) {
        return chatRepository.findByOwnerIdAndClientId(ownerId, clientId)
                .orElseGet(() -> {
                    Chat chat = new Chat();
                    chat.setOwnerId(ownerId);
                    chat.setClientId(clientId);
                    return chatRepository.saveAndFlush(chat);
                });
    }

    // ✅ Send a message (creates chat if needed, updates last message)
    @Transactional
    public Message sendMessage(SendMessageRequest req) {
        Chat chat = getOrCreateChat(req.getOwnerId(), req.getClientId());

        Message message = new Message();
        message.setChatId(chat.getChatId());
        message.setSenderId(req.getSenderId());
        message.setMessageText(req.getMessageText());
        message.setIsRead(false);
        Message saved = messageRepository.saveAndFlush(message);

        chat.setLastMessage(req.getMessageText());
        chat.setLastMessageTime(LocalDateTime.now());
        chatRepository.saveAndFlush(chat);

        return saved;
    }

    public List<Chat> getOwnerChats(Long ownerId) {
        return chatRepository.findByOwnerIdOrderByLastMessageTimeDesc(ownerId);
    }

    public List<Chat> getClientChats(Long clientId) {
        return chatRepository.findByClientIdOrderByLastMessageTimeDesc(clientId);
    }

    public List<Message> getMessages(Long chatId) {
        return messageRepository.findByChatIdOrderByMessageIdAsc(chatId);
    }

    @Transactional
    public void markRead(Long chatId, Long readerId) {
        List<Message> messages = messageRepository.findByChatIdOrderByMessageIdAsc(chatId);
        for (Message m : messages) {
            if (!readerId.equals(m.getSenderId()) && Boolean.FALSE.equals(m.getIsRead())) {
                m.setIsRead(true);
            }
        }
        messageRepository.saveAll(messages);
    }
}
