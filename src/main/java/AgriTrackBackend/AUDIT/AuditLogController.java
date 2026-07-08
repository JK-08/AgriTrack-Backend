package AgriTrackBackend.AUDIT;

import AgriTrackBackend.COMMON.PageResponse;
import AgriTrackBackend.SECURITY.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Read-only view over the audit trail. A caller can only see their own
 * activity (userId is forced to CurrentUser.id(), never trusted from the
 * client) — there's no admin/superuser role in this system yet to grant
 * broader access to.
 */
@RestController
@RequestMapping("/api/v1/audit")
@CrossOrigin
@Tag(name = "Audit Log", description = "Read-only activity trail. A caller only ever sees their own activity — userId is always taken from the JWT.")
public class AuditLogController {

    @Autowired
    private AuditService auditService;

    @Operation(summary = "Paged/search/filter/sort my activity", description = "?entityType/?entityId/?action filter exactly.")
    @GetMapping("/my-activity")
    public PageResponse<AuditLog> myActivity(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        return auditService.search(CurrentUser.id(), entityType, entityId, action, page, size, sortBy, sortDir);
    }
}
