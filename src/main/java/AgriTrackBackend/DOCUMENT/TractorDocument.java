package AgriTrackBackend.DOCUMENT;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "TRACTOR_DOCUMENTS")
public class TractorDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DOCUMENT_ID")
    private Long documentId;

    @Column(name = "TRACTOR_ID", nullable = false)
    private Long tractorId;

    @Column(name = "OWNER_ID", nullable = false)
    private Long ownerId;

    // RC / INSURANCE / PERMIT / PUC / FITNESS
    @Column(name = "DOCUMENT_TYPE", length = 20, nullable = false)
    private String documentType;

    @Column(name = "DOCUMENT_NUMBER", length = 100)
    private String documentNumber;

    @Column(name = "FILE_URL", length = 500, nullable = false)
    private String fileUrl;

    @Column(name = "ISSUE_DATE")
    private LocalDate issueDate;

    @Column(name = "EXPIRY_DATE")
    private LocalDate expiryDate;

    @Column(name = "REMINDER_SENT", nullable = false)
    private Boolean reminderSent = false;

    @Column(name = "NOTES", length = 500)
    private String notes;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
