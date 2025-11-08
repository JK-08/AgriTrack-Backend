package com.example.Agritrack.Banners.Controller;

import com.example.Agritrack.Banners.Model.Banner;
import com.example.Agritrack.Banners.BannerService.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banner")
@CrossOrigin(origins = "*")
public class BannerController {

    @Autowired
    private BannerService bannerService;

    @PostMapping("/add")
    public ResponseEntity<?> addBanner(@RequestBody Banner banner) {
        return bannerService.addBanner(banner);
    }

    @GetMapping("/all")
    public List<Banner> getAllBanners() {
        return bannerService.getAllBanners();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBannerById(@PathVariable Long id) {
        return bannerService.getBannerById(id);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateBanner(@PathVariable Long id, @RequestBody Banner banner) {
        return bannerService.updateBanner(id, banner);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteBanner(@PathVariable Long id) {
        return bannerService.deleteBanner(id);
    }
}
