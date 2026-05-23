package AgriTrackBackend.NOTIFICATION;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notification")
@CrossOrigin
public class NotificationController {

    @Autowired
    private NotificationService service;

    // CREATE TEMPLATE
    @PostMapping("/create")
    public NotificationEntity create(
            @RequestBody NotificationEntity entity
    ) {
        return service.save(entity);
    }

    // GET ALL
    @GetMapping("/getAll")
    public List<NotificationEntity> getAll() {
        return service.getAll();
    }

    // SEND TO USER
    @PostMapping("/send")
    public String send(
            @RequestBody SendNotificationRequest request
    ) throws Exception {

        service.sendToUser(
                request.getUserId(),
                request
        );

        return "Notification Sent Successfully";
    }
}