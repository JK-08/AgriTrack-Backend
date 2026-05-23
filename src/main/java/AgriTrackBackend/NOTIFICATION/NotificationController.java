package AgriTrackBackend.NOTIFICATION;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/notification")
@CrossOrigin
public class NotificationController {

    @Autowired
    private NotificationService service;

    @Autowired
    private NotificationRepository repository;

    @Autowired
    private UserNotificationTokenRepository tokenRepository;

    // CREATE
    @PostMapping("/create")
    public NotificationEntity create(
            @RequestBody SendNotificationRequest request
    ) {

        NotificationEntity entity =
                new NotificationEntity();

        entity.setUserId(request.getUserId());
        entity.setTitle(request.getTitle());
        entity.setSubtitle(request.getSubtitle());
        entity.setImageUrl(request.getImageUrl());
        entity.setScreenName(request.getScreenName());
        entity.setTimerSeconds(
                request.getTimerSeconds()
        );
        entity.setNotificationType(
                request.getNotificationType()
        );
        entity.setClickAction(
                request.getClickAction()
        );

        if (request.getSendAt() != null &&
                !request.getSendAt().isEmpty()) {

            entity.setSendAt(
                    LocalDateTime.parse(
                            request.getSendAt()
                    )
            );
        }

        entity.setIsActive(true);

        return service.save(entity);
    }

    // GET ALL
    @GetMapping("/getAll")
    public List<NotificationEntity> getAll() {

        return service.getAll();
    }

    // GET BY ID
    @GetMapping("/getById/{id}")
    public NotificationEntity getById(
            @PathVariable Long id
    ) {

        return repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Notification Not Found"
                        )
                );
    }

    // SEND NOW
    @PostMapping("/sendNow/{id}")
    public String sendNow(
            @PathVariable Long id
    ) throws Exception {

        NotificationEntity entity =
                repository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Notification Not Found"
                                )
                        );

        service.sendToUser(entity);

        return "Notification Sent Successfully";
    }

    // SAVE TOKEN
    @PostMapping("/saveToken")
    public UserNotificationToken saveToken(
            @RequestBody UserNotificationToken token
    ) {

        return tokenRepository.save(token);
    }

    // DISABLE TOKEN
    @PutMapping("/disableToken/{id}")
    public String disableToken(
            @PathVariable Long id
    ) {

        UserNotificationToken token =
                tokenRepository.findById(id)
                        .orElseThrow();

        token.setIsActive(false);

        tokenRepository.save(token);

        return "Token Disabled";
    }
    // UPDATE NOTIFICATION
    @PutMapping("/update/{id}")
    public NotificationEntity update(
            @PathVariable Long id,
            @RequestBody SendNotificationRequest request
    ) {

        NotificationEntity entity =
                repository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Notification Not Found"
                                )
                        );

        entity.setUserId(request.getUserId());

        entity.setTitle(request.getTitle());

        entity.setSubtitle(request.getSubtitle());

        entity.setImageUrl(request.getImageUrl());

        entity.setScreenName(request.getScreenName());

        entity.setTimerSeconds(
                request.getTimerSeconds()
        );

        entity.setNotificationType(
                request.getNotificationType()
        );

        entity.setClickAction(
                request.getClickAction()
        );

        if (request.getSendAt() != null &&
                !request.getSendAt().isEmpty()) {

            entity.setSendAt(
                    LocalDateTime.parse(
                            request.getSendAt()
                    )
            );
        }

        return repository.save(entity);
    }


    // DELETE NOTIFICATION
    @DeleteMapping("/delete/{id}")
    public String delete(
            @PathVariable Long id
    ) {

        repository.deleteById(id);

        return "Notification Deleted Successfully";
    }


    // DISABLE NOTIFICATION
    @PutMapping("/disable/{id}")
    public String disable(
            @PathVariable Long id
    ) {

        NotificationEntity entity =
                repository.findById(id)
                        .orElseThrow();

        entity.setIsActive(false);

        repository.save(entity);

        return "Notification Disabled";
    }


    // ENABLE NOTIFICATION
    @PutMapping("/enable/{id}")
    public String enable(
            @PathVariable Long id
    ) {

        NotificationEntity entity =
                repository.findById(id)
                        .orElseThrow();

        entity.setIsActive(true);

        repository.save(entity);

        return "Notification Enabled";
    }


    // GET TOKENS BY USER ID
    @GetMapping("/tokens/{userId}")
    public List<UserNotificationToken> getTokens(
            @PathVariable Long userId
    ) {

        return tokenRepository.findByUserId(userId);
    }


    // DELETE TOKEN
    @DeleteMapping("/deleteToken/{id}")
    public String deleteToken(
            @PathVariable Long id
    ) {

        tokenRepository.deleteById(id);

        return "Token Deleted Successfully";
    }
    // ENABLE TOKEN
    @PutMapping("/enableToken/{id}")
    public String enableToken(
            @PathVariable Long id
    ) {

        UserNotificationToken token =
                tokenRepository.findById(id)
                        .orElseThrow();

        token.setIsActive(true);

        tokenRepository.save(token);

        return "Token Enabled";
    }
}