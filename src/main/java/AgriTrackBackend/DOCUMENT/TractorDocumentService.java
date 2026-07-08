package AgriTrackBackend.DOCUMENT;

import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.COMMON.PageResponse;
import AgriTrackBackend.COMMON.PaginationUtil;
import AgriTrackBackend.EXCEPTION.ForbiddenException;
import AgriTrackBackend.EXCEPTION.ResourceNotFoundException;
import AgriTrackBackend.NOTIFICATION.NotificationEntity;
import AgriTrackBackend.NOTIFICATION.NotificationRepository;
import AgriTrackBackend.SECURITY.CurrentUser;
import AgriTrackBackend.TRACTOR.Tractor;
import AgriTrackBackend.TRACTOR.TractorRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TractorDocumentService {

    @Autowired
    private TractorDocumentRepository repository;

    @Autowired
    private TractorRepository tractorRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuditService auditService;

    // documents expiring within this many days trigger a renewal reminder
    private static final int REMINDER_WINDOW_DAYS = 30;

    private Tractor loadTractor(Long tractorId) {
        return tractorRepository.findById(tractorId)
                .orElseThrow(() -> new ResourceNotFoundException("Tractor not found with id: " + tractorId));
    }

    // ✅ Upload (first time) or Replace (re-upload) — one document per
    // tractor+type, enforced by the DB unique constraint, so this is a
    // natural upsert rather than two separate code paths.
    @Transactional
    public TractorDocument upload(TractorDocumentRequest request) {
        Tractor tractor = loadTractor(request.getTractorId());
        CurrentUser.requireSelf(tractor.getOwnerId());

        TractorDocument doc = repository.findByTractorIdAndDocumentType(request.getTractorId(), request.getDocumentType())
                .orElseGet(TractorDocument::new);
        boolean isNew = doc.getDocumentId() == null;
        String before = isNew ? null : auditService.snapshot(doc);

        doc.setTractorId(request.getTractorId());
        doc.setOwnerId(tractor.getOwnerId());
        doc.setDocumentType(request.getDocumentType().toUpperCase());
        doc.setFileUrl(request.getFileUrl());
        doc.setDocumentNumber(request.getDocumentNumber());
        doc.setIssueDate(request.getIssueDate());
        doc.setExpiryDate(request.getExpiryDate());
        doc.setNotes(request.getNotes());
        doc.setReminderSent(false); // a fresh upload/replace resets any prior expiry reminder

        TractorDocument saved = repository.saveAndFlush(doc);
        auditService.logRaw(isNew ? AuditAction.CREATE : AuditAction.UPDATE,
                "TractorDocument", saved.getDocumentId(), before, saved);
        return saved;
    }

    public List<TractorDocument> getByTractor(Long tractorId) {
        Tractor tractor = loadTractor(tractorId);
        CurrentUser.requireSelf(tractor.getOwnerId());
        return repository.findByTractorIdOrderByDocumentTypeAsc(tractorId);
    }

    public PageResponse<TractorDocument> searchPaged(Long ownerId, String documentType, Long tractorId,
                                                       LocalDate expiringBefore,
                                                       Integer page, Integer size, String sortBy, String sortDir) {
        CurrentUser.requireSelf(ownerId);
        Pageable pageable = PaginationUtil.build(page, size, sortBy, sortDir, "expiryDate");
        String dt = (documentType == null || documentType.isBlank()) ? null : documentType.toUpperCase();
        return PageResponse.of(repository.search(ownerId, dt, tractorId, expiringBefore, pageable));
    }

    public TractorDocument getById(Long id) {
        TractorDocument doc = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));
        assertOwner(doc);
        return doc;
    }

    private void assertOwner(TractorDocument doc) {
        if (doc.getOwnerId() == null || !doc.getOwnerId().equals(CurrentUser.id())) {
            throw new ForbiddenException("You do not have access to this document");
        }
    }

    @Transactional
    public void deleteById(Long id) {
        TractorDocument existing = getById(id);
        String before = auditService.snapshot(existing);
        repository.delete(existing);
        auditService.logRaw(AuditAction.DELETE, "TractorDocument", id, before, null);
    }

    // ✅ Runs once a day — reuses the NOTIFICATIONS table + the existing
    // NotificationService.autoSendNotifications() scheduler to actually
    // deliver the push, instead of duplicating any send logic here.
    @Scheduled(cron = "0 0 8 * * *") // 08:00 server time daily
    @Transactional
    public void sendExpiryReminders() {
        LocalDate today = LocalDate.now();
        LocalDate windowEnd = today.plusDays(REMINDER_WINDOW_DAYS);
        List<TractorDocument> expiring = repository.findByExpiryDateBetweenAndReminderSentFalse(today, windowEnd);

        for (TractorDocument doc : expiring) {
            NotificationEntity notification = new NotificationEntity();
            notification.setUserId(doc.getOwnerId());
            notification.setTitle("Document renewal due soon");
            notification.setSubtitle(String.format("%s for tractor #%d expires on %s — renew it soon.",
                    doc.getDocumentType(), doc.getTractorId(), doc.getExpiryDate()));
            notification.setNotificationType("DOCUMENT_EXPIRY");
            notification.setScreenName("TractorDocuments");
            notification.setIsActive(true);
            notification.setIsSent(false);
            notification.setSendAt(LocalDateTime.now());
            notificationRepository.save(notification);

            doc.setReminderSent(true);
            repository.save(doc);
            auditService.log(AuditAction.STATUS_CHANGE, "TractorDocument", doc.getDocumentId(), null,
                    java.util.Map.of("reminderSent", true));
        }
    }
}
