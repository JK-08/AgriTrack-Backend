package com.example.Agritrack.CompanyNames.Model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "company_names")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String shortName;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String logo;

    @Column(nullable = true)
    private String websiteLink;

    @Column(nullable = true)
    private String baseUrl;

    @Column(nullable = false)
    private String address1;

    private String address2;

    @Column(nullable = false)
    private String contactNumber1;

    private String contactNumber2;

    private String contactNumber3;

    private String facebook;
    private String instagram;
    private String youtube;
}
