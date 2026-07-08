package AgriTrackBackend.NOTIFICATION;

import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.SECURITY.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Module 6 — per-user notification channel preferences. PUSH and IN_APP are
 * enforced today (see NotificationService#sendToUser); SMS/EMAIL columns are
 * stored so the preference UI/API is stable once those channels ship, but
 * they have no effect yet since no SMS/Email sender exists.
 */
@Service
public class NotificationPreferenceService {

    @Autowired
    private NotificationPreferenceRepository repository;

    @Autowired
    private AuditService auditService;

    /** Returns one row per known notification type, synthesizing defaults for any the user hasn't customized. */
    public List<NotificationPreference> getForUser(Long userId) {
        CurrentUser.requireSelf(userId);
        Map<String, NotificationPreference> existing = new HashMap<>();
        for (NotificationPreference p : repository.findByUserId(userId)) {
            existing.put(p.getNotificationType(), p);
        }
        List<NotificationPreference> result = new ArrayList<>();
        for (String type : NotificationType.ALL) {
            result.add(existing.getOrDefault(type, defaultFor(userId, type)));
        }
        return result;
    }

    private NotificationPreference defaultFor(Long userId, String type) {
        NotificationPreference p = new NotificationPreference();
        p.setUserId(userId);
        p.setNotificationType(type);
        p.setPushEnabled(true);
        p.setInAppEnabled(true);
        p.setSmsEnabled(false);
        p.setEmailEnabled(false);
        return p;
    }

    @Transactional
    public NotificationPreference upsert(Long userId, NotificationPreferenceRequest request) {
        CurrentUser.requireSelf(userId);
        NotificationPreference pref = repository.findByUserIdAndNotificationType(userId, request.getNotificationType())
                .orElseGet(NotificationPreference::new);
        boolean isNew = pref.getPreferenceId() == null;
        String before = isNew ? null : auditService.snapshot(pref);

        pref.setUserId(userId);
        pref.setNotificationType(request.getNotificationType());
        pref.setPushEnabled(request.isPushEnabled());
        pref.setInAppEnabled(request.isInAppEnabled());
        pref.setSmsEnabled(request.isSmsEnabled());
        pref.setEmailEnabled(request.isEmailEnabled());
        pref.setUpdatedAt(LocalDateTime.now());

        NotificationPreference saved = repository.saveAndFlush(pref);
        auditService.logRaw(isNew ? AuditAction.CREATE : AuditAction.UPDATE, "NotificationPreference",
                saved.getPreferenceId(), before, saved);
        return saved;
    }

    /** Used by NotificationService before pushing — defaults to enabled when no explicit preference exists. */
    public boolean isPushEnabled(Long userId, String notificationType) {
        if (notificationType == null) return true;
        return repository.findByUserIdAndNotificationType(userId, notificationType)
                .map(NotificationPreference::getPushEnabled)
                .orElse(true);
    }
}
