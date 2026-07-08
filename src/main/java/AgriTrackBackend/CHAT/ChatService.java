package AgriTrackBackend.CHAT;

import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.COMMON.PageResponse;
import AgriTrackBackend.COMMON.PaginationUtil;
import AgriTrackBackend.EXCEPTION.ForbiddenException;
import AgriTrackBackend.EXCEPTION.ResourceNotFoundException;
import AgriTrackBackend.SECURITY.CurrentUser;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private AuditService auditService;

    // only the two participants of a chat may touch it
    private void assertParticipant(Chat chat) {
        Long me = CurrentUser.id();
        if (!chat.getOwnerId().equals(me) && !chat.getClientId().equals(me)) {
            throw new ForbiddenException("You do not have access to this chat");
        }
    }

    // ✅ Find existing chat or create a new one — caller must be one of the two participants
    @Transactional
    public Chat getOrCreateChat(Long ownerId, Long clientId) {
        Long me = CurrentUser.id();
        if (!ownerId.equals(me) && !clientId.equals(me)) {
            throw new ForbiddenException("You can only start a chat you are part of");
        }
        return chatRepository.findByOwnerIdAndClientId(ownerId, clientId)
                .orElseGet(() -> {
                    Chat chat = new Chat();
                    chat.setOwnerId(ownerId);
                    chat.setClientId(clientId);
                    Chat saved = chatRepository.saveAndFlush(chat);
                    auditService.log(AuditAction.CREATE, "Chat", saved.getChatId(), null,
                            Map.of("ownerId", ownerId, "clientId", clientId));
                    return saved;
                });
    }

    // ✅ Send a message (creates chat if needed, updates last message)
    @Transactional
    public Message sendMessage(SendMessageRequest req) {
        Chat chat = getOrCreateChat(req.getOwnerId(), req.getClientId());

        Message message = new Message();
        message.setChatId(chat.getChatId());
        // ✅ never trust a client-supplied senderId — always the caller
        message.setSenderId(CurrentUser.id());
        message.setMessageText(req.getMessageText());
        message.setIsRead(false);
        Message saved = messageRepository.saveAndFlush(message);

        chat.setLastMessage(req.getMessageText());
        chat.setLastMessageTime(LocalDateTime.now());
        chatRepository.saveAndFlush(chat);

        // ✅ audit the event, never the message content itself (personal comms)
        auditService.log(AuditAction.CREATE, "Message", saved.getMessageId(), null,
                Map.of("chatId", saved.getChatId(), "senderId", saved.getSenderId()));

        return saved;
    }

    public List<Chat> getOwnerChats(Long ownerId) {
        CurrentUser.requireSelf(ownerId);
        return chatRepository.findByOwnerIdOrderByLastMessageTimeDesc(ownerId);
    }

    public List<Chat> getClientChats(Long clientId) {
        CurrentUser.requireSelf(clientId);
        return chatRepository.findByClientIdOrderByLastMessageTimeDesc(clientId);
    }

    public List<Message> getMessages(Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found with id: " + chatId));
        assertParticipant(chat);
        return messageRepository.findByChatIdOrderByMessageIdAsc(chatId);
    }

    public PageResponse<Message> searchMessagesPaged(Long chatId, String search, Boolean isRead,
                                                       Integer page, Integer size, String sortBy, String sortDir) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found with id: " + chatId));
        assertParticipant(chat);
        Pageable pageable = PaginationUtil.build(page, size, sortBy, sortDir, "createdAt");
        String s = (search == null || search.isBlank()) ? null : search;
        return PageResponse.of(messageRepository.search(chatId, s, isRead, pageable));
    }

    @Transactional
    public void markRead(Long chatId, Long readerId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found with id: " + chatId));
        assertParticipant(chat);
        CurrentUser.requireSelf(readerId);

        List<Message> messages = messageRepository.findByChatIdOrderByMessageIdAsc(chatId);
        for (Message m : messages) {
            if (!readerId.equals(m.getSenderId()) && Boolean.FALSE.equals(m.getIsRead())) {
                m.setIsRead(true);
            }
        }
        messageRepository.saveAll(messages);
    }
}
