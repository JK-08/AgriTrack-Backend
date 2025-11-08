package com.example.Agritrack.Banners.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.Agritrack.Banners.Model.Banner;

public interface BannerRepository extends JpaRepository<Banner, Long> {
}
