package com.example.Agritrack.media.video.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "VIDEO")
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TITLE")
    private String title;

    @Column(name = "SUBTITLE")
    private String subtitle;

    @Column(name = "NAME")
    private String name;

    @Column(name = "VIDEO_URL")
    private String video_url;

    @Column(name = "ACTIVE")
    private boolean active;

    // ✅ Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVideo_url() { return video_url; }
    public void setVideo_url(String video_url) { this.video_url = video_url; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
