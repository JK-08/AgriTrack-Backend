package AgriTrackBackend.CHAT;

import AgriTrackBackend.COMMON.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@CrossOrigin
@Tag(name = "Chat", description = "1:1 conversations between an owner and a client. Only participants can access a chat.")
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

    @Operation(summary = "Paged/search/filter/sort messages in a chat",
            description = "?search matches message text. ?isRead filters exactly. Additive.")
    @GetMapping("/messages-paged/{chatId}")
    public PageResponse<Message> messagesPaged(
            @PathVariable Long chatId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        return service.searchMessagesPaged(chatId, search, isRead, page, size, sortBy, sortDir);
    }

    // ✅ Mark conversation read
    @PutMapping("/markRead/{chatId}")
    public String markRead(@PathVariable Long chatId, @RequestParam Long readerId) {
        service.markRead(chatId, readerId);
        return "Marked as read";
    }
}
