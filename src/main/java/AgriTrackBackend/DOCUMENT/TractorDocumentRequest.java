package AgriTrackBackend.DOCUMENT;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TractorDocumentRequest {

    @NotNull(message = "tractorId is required")
    private Long tractorId;

    @NotBlank(message = "documentType is required")
    @Pattern(regexp = "RC|INSURANCE|PERMIT|PUC|FITNESS",
            message = "documentType must be one of RC, INSURANCE, PERMIT, PUC, FITNESS")
    private String documentType;

    @NotBlank(message = "fileUrl is required — upload the file first via /document/upload")
    private String fileUrl;

    private String documentNumber;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String notes;
}
