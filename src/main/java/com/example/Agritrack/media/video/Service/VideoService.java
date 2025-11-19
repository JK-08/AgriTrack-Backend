package com.example.Agritrack.media.video.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.Agritrack.media.video.Entity.Video;
import com.example.Agritrack.media.video.Repository.VideoRepo;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class VideoService {

    @Autowired
    private VideoRepo videoRepo;

    @Autowired
    private Cloudinary cloudinary;

    @Autowired
    private EntityManager entityManager;

    @Transactional
    public Video uploadVideo(MultipartFile file, String title, String subtitle, String name, boolean active)
            throws IOException {

        // 1️⃣ Reset ID if table empty
        long count = (long) entityManager.createQuery("SELECT COUNT(v) FROM Video v").getSingleResult();
        if (count == 0) {
            entityManager.createNativeQuery("DBCC CHECKIDENT ('VIDEO', RESEED, 0)").executeUpdate();
        }

        // 2️⃣ Upload to Cloudinary
        String folderPath = "videos/";
        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "resource_type", "video",
                        "folder", folderPath,
                        "public_id", name, // 👈 custom video name
                        "overwrite", true,
                        "access_mode", "public"
                )
        );

        // 3️⃣ Save video in DB
        Video video = new Video();
        video.setTitle(title);
        video.setSubtitle(subtitle);
        video.setName(name);
        video.setActive(active);
        video.setVideo_url(uploadResult.get("secure_url").toString());

        return videoRepo.save(video);
    }

    // 🟢 Get All Videos
    public java.util.List<Video> getAllVideos() {
        return videoRepo.findAll();
    }

    // 🟡 Get Video by ID
    public Video getVideoById(Long id) {
        return videoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found with ID: " + id));
    }

    // 🟠 Update Video (Partial update)
    public Video updateVideo(Long id, String title, String subtitle, String name, Boolean active) {
        Video video = videoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found with ID: " + id));

        if (title != null) video.setTitle(title);
        if (subtitle != null) video.setSubtitle(subtitle);
        if (name != null) video.setName(name);
        if (active != null) video.setActive(active);

        return videoRepo.save(video);
    }

    // 🔴 Delete Video by ID
    public void deleteVideo(Long id) {
        videoRepo.deleteById(id);
    }
}
