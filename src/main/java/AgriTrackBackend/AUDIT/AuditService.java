package AgriTrackBackend.AUDIT;

import AgriTrackBackend.COMMON.PageResponse;
import AgriTrackBackend.COMMON.PaginationUtil;
import AgriTrackBackend.SECURITY.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Central write path for the activity/audit trail. Every mutating service
 * method that matters (create/update/delete/status-change on a business
 * entity, or a security-sensitive event like login/logout/password reset)
 * calls one of these instead of writing AUDIT_LOGS rows directly, so the
 * shape is always consistent and always includes who/when/what.
 *
 * Audit writes are best-effort: a failure to record an audit entry must
 * never break the business operation it's describing, so every path here
 * swallows its own exceptions after logging them.
 */
@Service
public class AuditService {

    @Autowired
    private AuditLogRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    public void log(String action, String entityType, Object entityId, Object before, Object after) {
        logRaw(action, entityType, entityId, toJson(before), toJson(after));
    }

    /**
     * Use when the "before" state has to be captured via {@link #snapshot}
     * BEFORE the entity is mutated in place (the common case: services fetch
     * an entity, call setters on it, then save() — by the time save()
     * returns, the in-memory object no longer reflects the pre-change state).
     */
    public void logRaw(String action, String entityType, Object entityId, String beforeJson, Object after) {
        try {
            AuditLog log = new AuditLog();
            log.setUserId(currentUserIdOrNull());
            log.setAction(action);
            log.setEntityType(entityType);
            log.setEntityId(entityId == null ? null : String.valueOf(entityId));
            log.setBeforeValue(beforeJson);
            log.setAfterValue(toJson(after));
            repository.save(log);
        } catch (Exception e) {
            System.out.println("[AUDIT] Failed to record audit log for " + entityType + "/" + entityId + ": " + e.getMessage());
        }
    }

    /** Capture a JSON snapshot of an entity's current state, to diff against later. */
    public String snapshot(Object entity) {
        return toJson(entity);
    }

    /** For actions with no single "entity" (e.g. login) — entityId doubles as a free-text identifier. */
    public void logEvent(String action, String entityType, String identifier) {
        log(action, entityType, identifier, null, null);
    }

    public PageResponse<AuditLog> search(Long userId, String entityType, String entityId, String action,
                                          Integer page, Integer size, String sortBy, String sortDir) {
        Pageable pageable = PaginationUtil.build(page, size, sortBy, sortDir, "createdAt");
        return PageResponse.of(repository.search(userId, entityType, entityId, action, pageable));
    }

    private Long currentUserIdOrNull() {
        try {
            return CurrentUser.id();
        } catch (Exception e) {
            return null; // no authenticated user in context (e.g. failed login, public endpoint)
        }
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "\"<unserializable>\"";
        }
    }
}
