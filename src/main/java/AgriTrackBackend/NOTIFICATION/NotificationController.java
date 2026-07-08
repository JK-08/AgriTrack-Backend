package AgriTrackBackend.NOTIFICATION;

import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.COMMON.PageResponse;
import AgriTrackBackend.EXCEPTION.ForbiddenException;
import AgriTrackBackend.EXCEPTION.ResourceNotFoundException;
import AgriTrackBackend.SECURITY.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/notification")
@CrossOrigin
@Tag(name = "Notifications", description = "Push notifications sent to users, plus device-token management")
public class NotificationController {

    @Autowired
    private NotificationService service;

    @Autowired
    private NotificationRepository repository;

    @Autowired
    private UserNotificationTokenRepository tokenRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private NotificationPreferenceService preferenceService;

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

    // GET BY USER
    @GetMapping("/user/{userId}")
    public List<NotificationEntity> getByUser(
            @PathVariable Long userId
    ) {

        return service.getByUser(userId);
    }

    @Operation(summary = "Paged/search/filter/sort notification list",
            description = "?search matches title. ?notificationType/?isSent filter exactly. Additive.")
    @GetMapping("/search-paged/{userId}")
    public PageResponse<NotificationEntity> searchPaged(
            @PathVariable Long userId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String notificationType,
            @RequestParam(required = false) Boolean isSent,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        return service.searchPaged(userId, search, notificationType, isSent, page, size, sortBy, sortDir);
    }

    // ✅ shared guard for every endpoint below that takes a notification id directly:
    // only the recipient, or an OWNER (who may have sent it), may touch it
    private NotificationEntity findOwned(Long id) {
        NotificationEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification Not Found"));
        Long me = CurrentUser.id();
        boolean isRecipient = entity.getUserId() != null && entity.getUserId().equals(me);
        if (!isRecipient && !CurrentUser.isRole("OWNER")) {
            throw new ForbiddenException("You do not have access to this notification");
        }
        return entity;
    }

    // GET BY ID
    @GetMapping("/getById/{id}")
    public NotificationEntity getById(
            @PathVariable Long id
    ) {
        return findOwned(id);
    }

    // SEND NOW
    @PostMapping("/sendNow/{id}")
    public String sendNow(
            @PathVariable Long id
    ) throws Exception {

        NotificationEntity entity = findOwned(id);

        service.sendToUser(entity);

        return "Notification Sent Successfully";
    }

    // SAVE TOKEN — always for the caller's own device
    @PostMapping("/saveToken")
    public UserNotificationToken saveToken(
            @RequestBody UserNotificationToken token
    ) {
        token.setUserId(CurrentUser.id());
        return tokenRepository.save(token);
    }

    // DISABLE TOKEN
    @PutMapping("/disableToken/{id}")
    public String disableToken(
            @PathVariable Long id
    ) {

        UserNotificationToken token =
                tokenRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Token not found"));
        CurrentUser.requireSelf(token.getUserId());

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

        NotificationEntity entity = findOwned(id);
        String before = auditService.snapshot(entity);

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

        NotificationEntity saved = repository.save(entity);
        auditService.logRaw(AuditAction.UPDATE, "Notification", id, before, saved);
        return saved;
    }


    // DELETE NOTIFICATION
    @DeleteMapping("/delete/{id}")
    public String delete(
            @PathVariable Long id
    ) {

        NotificationEntity entity = findOwned(id);
        String before = auditService.snapshot(entity);
        repository.delete(entity);
        auditService.logRaw(AuditAction.DELETE, "Notification", id, before, null);

        return "Notification Deleted Successfully";
    }


    // DISABLE NOTIFICATION
    @PutMapping("/disable/{id}")
    public String disable(
            @PathVariable Long id
    ) {

        NotificationEntity entity = findOwned(id);
        String before = auditService.snapshot(entity);

        entity.setIsActive(false);

        NotificationEntity saved = repository.save(entity);
        auditService.logRaw(AuditAction.STATUS_CHANGE, "Notification", id, before, saved);

        return "Notification Disabled";
    }


    // ENABLE NOTIFICATION
    @PutMapping("/enable/{id}")
    public String enable(
            @PathVariable Long id
    ) {

        NotificationEntity entity = findOwned(id);
        String before = auditService.snapshot(entity);

        entity.setIsActive(true);

        NotificationEntity saved = repository.save(entity);
        auditService.logRaw(AuditAction.STATUS_CHANGE, "Notification", id, before, saved);

        return "Notification Enabled";
    }


    // GET TOKENS BY USER ID
    @GetMapping("/tokens/{userId}")
    public List<UserNotificationToken> getTokens(
            @PathVariable Long userId
    ) {
        CurrentUser.requireSelf(userId);
        return tokenRepository.findByUserId(userId);
    }


    // DELETE TOKEN
    @DeleteMapping("/deleteToken/{id}")
    public String deleteToken(
            @PathVariable Long id
    ) {

        UserNotificationToken token = tokenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found"));
        CurrentUser.requireSelf(token.getUserId());
        tokenRepository.delete(token);

        return "Token Deleted Successfully";
    }
    // ENABLE TOKEN
    @PutMapping("/enableToken/{id}")
    public String enableToken(
            @PathVariable Long id
    ) {

        UserNotificationToken token =
                tokenRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Token not found"));
        CurrentUser.requireSelf(token.getUserId());

        token.setIsActive(true);

        tokenRepository.save(token);

        return "Token Enabled";
    }

    // ✅ Module 6 — per-type Push/In-App/SMS/Email preferences
    @Operation(summary = "Get notification preferences", description = "Returns one row per known notification type "
            + "(BOOKING, PAYMENT, DOCUMENT_EXPIRY, RATE_ALERT, MAINTENANCE, PAYROLL, CHAT, GENERAL), synthesizing "
            + "defaults (push+in-app on, sms+email off) for any type the user hasn't customized.")
    @GetMapping("/preferences/{userId}")
    public List<NotificationPreference> getPreferences(@PathVariable Long userId) {
        return preferenceService.getForUser(userId);
    }

    @Operation(summary = "Update a notification preference", description = "Upserts the caller's Push/In-App/SMS/Email "
            + "preference for one notification type. SMS/Email flags are stored but have no effect yet — no sender "
            + "is wired up for those channels.")
    @PutMapping("/preferences/{userId}")
    public NotificationPreference updatePreference(
            @PathVariable Long userId,
            @Valid @RequestBody NotificationPreferenceRequest request
    ) {
        return preferenceService.upsert(userId, request);
    }
}