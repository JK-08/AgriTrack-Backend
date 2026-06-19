package AgriTrackBackend.CHAT;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@CrossOrigin
public class ChatController {

    @Autowired
    private ChatService service;

    // ✅ Start / fetch a conversation
    @PostMapping("/start")
    public Chat start(@RequestParam Long ownerId, @RequestParam Long clientId) {
        return service.getOrCreateChat(ownerId, clientId);
    }

    // ✅ Send message
    @PostMapping("/send")
    public Message send(@RequestBody SendMessageRequest request) {
        return service.sendMessage(request);
    }

    // ✅ Owner's conversation list
    @GetMapping("/owner/{ownerId}")
    public List<Chat> ownerChats(@PathVariable Long ownerId) {
        return service.getOwnerChats(ownerId);
    }

    // ✅ Client's conversation list
    @GetMapping("/client/{clientId}")
    public List<Chat> clientChats(@PathVariable Long clientId) {
        return service.getClientChats(clientId);
    }

    // ✅ Messages in a conversation
    @GetMapping("/messages/{chatId}")
    public List<Message> messages(@PathVariable Long chatId) {
        return service.getMessages(chatId);
    }

    // ✅ Mark conversation read
    @PutMapping("/markRead/{chatId}")
    public String markRead(@PathVariable Long chatId, @RequestParam Long readerId) {
        service.markRead(chatId, readerId);
        return "Marked as read";
    }
}
