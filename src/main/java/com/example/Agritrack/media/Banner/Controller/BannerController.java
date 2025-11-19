package com.example.Agritrack.media.Banner.Controller;

import com.example.Agritrack.media.Banner.Entity.Banner;
import com.example.Agritrack.media.Banner.Service.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/banner")
public class BannerController {

    @Autowired
    private BannerService bannerService;

    // 🟢 CREATE
    @PostMapping("/upload")
    public Banner upload(@RequestParam("file") MultipartFile file,
                         @RequestParam("title") String title,
                         @RequestParam("subtitle") String subtitle,
                         @RequestParam("name") String name,
                         @RequestParam("active") boolean active) throws IOException {
        return bannerService.uploadBanner(file, title, subtitle, name, active);
    }

    // 🟡 READ ALL
    @GetMapping("All")
    public List<Banner> getAll() {
        return bannerService.getAllBanners();
    }

    // 🟡 READ BY ID
    @GetMapping("/{id}")
    public Banner getById(@PathVariable Long id) {
        return bannerService.getBannerById(id);
    }

    // 🟠 UPDATE
    @PatchMapping("/{id}")
    public Banner update(@PathVariable Long id,
                         @RequestParam(required = false) String title,
                         @RequestParam(required = false) String subtitle,
                         @RequestParam(required = false) String name,
                         @RequestParam(required = false) boolean active) {
        return bannerService.updateBanner(id, title, subtitle, name, active);
    }

    // 🔴 DELETE
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) throws IOException {
        bannerService.deleteBanner(id);
        return "Banner deleted successfully!";
    }
}
