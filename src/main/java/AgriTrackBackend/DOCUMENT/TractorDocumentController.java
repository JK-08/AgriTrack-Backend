package AgriTrackBackend.DOCUMENT;

import AgriTrackBackend.CLOUDINARY.CloudinaryService;
import AgriTrackBackend.COMMON.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/document")
@CrossOrigin
@Tag(name = "Tractor Documents", description = "RC, Insurance, Permit, PUC, and Fitness certificates for a tractor — one document per type, with expiry tracking and automatic renewal reminders.")
public class TractorDocumentController {

    @Autowired
    private TractorDocumentService service;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Operation(summary = "Upload a document file", description = "Returns a URL to pass as fileUrl on upload/replace. Preview/Download both just use this same URL.")
    @PostMapping("/upload-file")
    public Map<String, String> uploadFile(@RequestParam MultipartFile file) {
        return Map.of("url", cloudinaryService.uploadImage(file));
    }

    @Operation(summary = "Upload or replace a document", description = "One document per tractor+type — calling this again for the same tractor+type replaces the existing record and resets its expiry-reminder flag.")
    @PostMapping("/upload")
    public TractorDocument upload(@Valid @RequestBody TractorDocumentRequest request) {
        return service.upload(request);
    }

    @Operation(summary = "All documents for a tractor")
    @GetMapping("/by-tractor/{tractorId}")
    public List<TractorDocument> byTractor(@PathVariable Long tractorId) {
        return service.getByTractor(tractorId);
    }

    @Operation(summary = "Paged/search/filter/sort document list", description = "?documentType/?tractorId filter exactly; ?expiringBefore filters documents expiring on or before that date.")
    @GetMapping("/search-paged/{ownerId}")
    public PageResponse<TractorDocument> searchPaged(
            @PathVariable Long ownerId,
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) Long tractorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiringBefore,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        return service.searchPaged(ownerId, documentType, tractorId, expiringBefore, page, size, sortBy, sortDir);
    }

    @Operation(summary = "Get a document by id")
    @GetMapping("/getById/{id}")
    public TractorDocument getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @Operation(summary = "Delete a document", description = "Only the owning owner may delete (403 otherwise).")
    @DeleteMapping("/deleteById/{id}")
    public String deleteById(@PathVariable Long id) {
        service.deleteById(id);
        return "Document deleted successfully";
    }
}
