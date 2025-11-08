package com.example.Agritrack.Banners.BannerService;

import com.example.Agritrack.Banners.Model.Banner;
import com.example.Agritrack.Banners.Repository.BannerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BannerService {

    @Autowired
    private BannerRepository bannerRepository;

    // Add Banner
    public ResponseEntity<?> addBanner(Banner banner) {
        if (banner.getImage() == null || banner.getImage().isEmpty() ||
                banner.getTitle() == null || banner.getTitle().isEmpty() ||
                banner.getSubtitle() == null || banner.getSubtitle().isEmpty()) {
            return ResponseEntity.ok("Image, title, and subtitle are mandatory!");
        }

        Banner saved = bannerRepository.save(banner);
        return ResponseEntity.ok(saved);
    }

    // Get all banners
    public List<Banner> getAllBanners() {
        return bannerRepository.findAll();
    }

    // Get banner by ID
    public ResponseEntity<?> getBannerById(Long id) {
        Optional<Banner> banner = bannerRepository.findById(id);
        if (banner.isPresent()) {
            return ResponseEntity.ok(banner.get());
        } else {
            return ResponseEntity.ok("Banner not found!");
        }
    }

    // ✅ Update Banner
    public ResponseEntity<?> updateBanner(Long id, Banner newBanner) {
        Optional<Banner> existing = bannerRepository.findById(id);

        if (existing.isPresent()) {
            Banner banner = existing.get();
            if (newBanner.getImage() != null) banner.setImage(newBanner.getImage());
            if (newBanner.getTitle() != null) banner.setTitle(newBanner.getTitle());
            if (newBanner.getSubtitle() != null) banner.setSubtitle(newBanner.getSubtitle());
            if (newBanner.getLink() != null) banner.setLink(newBanner.getLink());

            Banner updated = bannerRepository.save(banner);
            return ResponseEntity.ok(updated);
        } else {
            return ResponseEntity.ok("Banner not found!");
        }
    }

    // ✅ Delete Banner
    public ResponseEntity<?> deleteBanner(Long id) {
        if (bannerRepository.existsById(id)) {
            bannerRepository.deleteById(id);
            return ResponseEntity.ok("Banner deleted successfully!");
        } else {
            return ResponseEntity.ok("Banner not found!");
        }
    }
}
