package com.example.Agritrack.media.Banner.Service;

import com.example.Agritrack.media.Banner.Entity.Banner;
import com.example.Agritrack.media.Banner.Repository.BannerRepo;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class BannerService {

    @Autowired
    private Cloudinary cloudinary;

    @Autowired
    private BannerRepo bannerRepo;


    @Autowired
    private EntityManager entityManager; // 👈 Add here

    // 🟢 CREATE (Upload to Cloudinary + Save in DB + Reset ID if empty)
    @Transactional
    public Banner uploadBanner(MultipartFile file, String title, String subtitle, String name, boolean active)
            throws IOException {

        // 1️⃣ Reset ID if table is empty
        long count = (long) entityManager.createQuery("SELECT COUNT(b) FROM Banner b").getSingleResult();
        if (count == 0) {
            entityManager.createNativeQuery("DBCC CHECKIDENT ('BANNER', RESEED, 0)").executeUpdate();
        }

        // 2️⃣ Define folder + custom filename
        String folderPath = "banners";
        String publicId = name.replaceAll("\\s+", "_").toLowerCase(); // make it clean like 'banner_1'

        // 3️⃣ Upload to Cloudinary
        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "resource_type", "image",
                        "folder", folderPath,
                        "public_id", publicId,   // 👈 custom file name
                        "overwrite", true,       // 👈 replace if same name exists
                        "access_mode", "public"
                )
        );

        // 4️⃣ Save banner in DB
        Banner banner = new Banner();
        banner.setTitle(title);
        banner.setSubtitle(subtitle);
        banner.setName(name);
        banner.setActive(active);
        banner.setImage_url(uploadResult.get("secure_url").toString());

        return bannerRepo.save(banner);
    }

    // 🟡 READ — Get all banners
    public List<Banner> getAllBanners() {
        return bannerRepo.findAll();
    }

    // 🟡 READ — Get banner by ID
    public Banner getBannerById(Long id) {
        return bannerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner not found with ID: " + id));
    }

    // 🟠 UPDATE — Edit existing banner
    public Banner updateBanner(Long id, String title, String subtitle, String name, boolean active) {
        Banner banner = bannerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner not found with ID: " + id));

        if (title != null) banner.setTitle(title);
        if (subtitle != null) banner.setSubtitle(subtitle);
        if (name != null) banner.setName(name);
        banner.setActive(active);

        return bannerRepo.save(banner);
    }

    // 🔴 DELETE — Remove from Cloudinary + DB
    public void deleteBanner(Long id) throws IOException {
        Banner banner = bannerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner not found with ID: " + id));

        // Delete image from Cloudinary
        String publicId = extractPublicId(banner.getImage_url());
        cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));

        bannerRepo.delete(banner);
    }

    // Helper method to extract Cloudinary public ID
    private String extractPublicId(String url) {
        String[] parts = url.split("/");
        String filename = parts[parts.length - 1];
        return filename.substring(0, filename.lastIndexOf('.'));
    }
}
