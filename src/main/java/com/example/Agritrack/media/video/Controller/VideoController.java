package com.example.Agritrack.media.video.Controller;

import com.example.Agritrack.media.video.Entity.Video;
import com.example.Agritrack.media.video.Service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/videos")
@CrossOrigin(origins = "*")
public class VideoController {

    @Autowired
    private VideoService videoService;

    // 🟢 Upload Video
    @PostMapping("/upload")
    public ResponseEntity<Video> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("subtitle") String subtitle,
            @RequestParam("name") String name,
            @RequestParam("active") boolean active
    ) throws IOException {
        return ResponseEntity.ok(videoService.uploadVideo(file, title, subtitle, name, active));
    }

    // 🟣 Get All Videos
    @GetMapping("/all")
    public ResponseEntity<List<Video>> getAllVideos() {
        return ResponseEntity.ok(videoService.getAllVideos());
    }

    // 🟡 Get Video by ID
    @GetMapping("/{id}")
    public ResponseEntity<Video> getVideoById(@PathVariable Long id) {
        return ResponseEntity.ok(videoService.getVideoById(id));
    }

    // 🟠 Update Video (Partial update)
    @PatchMapping("/{id}")
    public ResponseEntity<Video> updateVideo(
            @PathVariable Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String subtitle,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean active
    ) {
        return ResponseEntity.ok(videoService.updateVideo(id, title, subtitle, name, active));
    }

    // 🔴 Delete Video by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteVideo(@PathVariable Long id) {
        videoService.deleteVideo(id);
        return ResponseEntity.ok("Video deleted successfully!");
    }
}
