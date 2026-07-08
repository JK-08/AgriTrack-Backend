package AgriTrackBackend.ONBOARDING;

import AgriTrackBackend.CLOUDINARY.CloudinaryService;
import AgriTrackBackend.COMMON.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/onboarding")
@CrossOrigin
@Tag(name = "Onboarding", description = "Public app-intro screens (title/subtitle/image). getAll and getById are public; everything else requires auth.")
public class OnboardingController {

    @Autowired
    private OnboardingService service;

    @Autowired
    private CloudinaryService cloudinaryService;

    // ✅ CREATE
    @PostMapping("/create")
    public Onboarding create(
            @RequestParam String title,
            @RequestParam String subtitle,
            @RequestParam MultipartFile image
    ) {
        String imageUrl = cloudinaryService.uploadImage(image);

        Onboarding ob = new Onboarding();
        ob.setTitle(title);
        ob.setSubtitle(subtitle);
        ob.setImageUrl(imageUrl);

        return service.save(ob);
    }

    // ✅ GET ALL
    @GetMapping("/getAll")
    public List<Onboarding> getAll() {
        return service.getAll();
    }

    @Operation(summary = "Paged/search/sort onboarding list", description = "?search matches title/subtitle. Additive.")
    @GetMapping("/search-paged")
    public PageResponse<Onboarding> searchPaged(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        return service.searchPaged(search, page, size, sortBy, sortDir);
    }

    // ✅ GET BY ID
    @GetMapping("/getById/{id}")
    public Onboarding getById(@PathVariable Long id) {
        return service.getById(id);
    }

    // ✅ UPDATE
    @PutMapping("/update/{id}")
    public Onboarding update(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String subtitle,
            @RequestParam(required = false) MultipartFile image
    ) {
        Onboarding existing = service.getById(id);

        existing.setTitle(title);
        existing.setSubtitle(subtitle);

        if (image != null && !image.isEmpty()) {
            String imageUrl = cloudinaryService.uploadImage(image);
            existing.setImageUrl(imageUrl);
        }

        return service.save(existing);
    }

    // ✅ DELETE BY ID
    @DeleteMapping("/deleteById/{id}")
    public String deleteById(@PathVariable Long id) {
        service.deleteById(id);
        return "Deleted Successfully";
    }

    // ✅ DELETE ALL
    @DeleteMapping("/deleteAll")
    public String deleteAll() {
        service.deleteAll();
        return "All records deleted successfully";
    }
}