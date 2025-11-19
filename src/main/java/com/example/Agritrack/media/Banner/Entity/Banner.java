package com.example.Agritrack.media.Banner.Entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="BANNER_IMAGE")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Banner {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id ;

    @Column(name = "TITLE")
    private String title ;

    @Column(name = "SUBTITLE")
    private String subtitle;

    @Column(name = "NAME")
    private String name;

    @Column(name = "IMAGE_URL")
    private String image_url;

    @Column(name = "ACTIVE")
    private boolean active;


}
