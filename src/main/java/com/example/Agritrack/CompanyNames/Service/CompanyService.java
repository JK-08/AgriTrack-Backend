package com.example.Agritrack.CompanyNames.Service;

import com.example.Agritrack.CompanyNames.Model.Company;
import com.example.Agritrack.CompanyNames.Repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CompanyService {

    @Autowired
    private CompanyRepository companyRepository;

    // Add new company
    public ResponseEntity<?> addCompany(Company company) {
        if (companyRepository.existsByShortName(company.getShortName())) {
            return ResponseEntity.ok("Company short name already exists!");
        }

        if (company.getShortName() == null || company.getFullName() == null ||
                company.getLogo() == null || company.getAddress1() == null ||
                company.getContactNumber1() == null) {
            return ResponseEntity.ok("Missing required fields!");
        }

        Company saved = companyRepository.save(company);
        return ResponseEntity.ok(saved);
    }

    // Get all companies
    public ResponseEntity<List<Company>> getAllCompanies() {
        return ResponseEntity.ok(companyRepository.findAll());
    }

    // Get company by ID
    public ResponseEntity<?> getCompanyById(Long id) {
        Optional<Company> company = companyRepository.findById(id);
        return company.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok("Company not found!"));
    }

    // Update company
    public ResponseEntity<?> updateCompany(Long id, Company companyDetails) {
        Optional<Company> company = companyRepository.findById(id);
        if (company.isEmpty()) {
            return ResponseEntity.ok("Company not found!");
        }

        Company existing = company.get();
        existing.setShortName(companyDetails.getShortName());
        existing.setFullName(companyDetails.getFullName());
        existing.setLogo(companyDetails.getLogo());
        existing.setWebsiteLink(companyDetails.getWebsiteLink());
        existing.setBaseUrl(companyDetails.getBaseUrl());
        existing.setAddress1(companyDetails.getAddress1());
        existing.setAddress2(companyDetails.getAddress2());
        existing.setContactNumber1(companyDetails.getContactNumber1());
        existing.setContactNumber2(companyDetails.getContactNumber2());
        existing.setContactNumber3(companyDetails.getContactNumber3());
        existing.setFacebook(companyDetails.getFacebook());
        existing.setInstagram(companyDetails.getInstagram());
        existing.setYoutube(companyDetails.getYoutube());

        companyRepository.save(existing);
        return ResponseEntity.ok(existing);
    }

    // Delete company
    public ResponseEntity<?> deleteCompany(Long id) {
        if (!companyRepository.existsById(id)) {
            return ResponseEntity.ok("Company not found!");
        }
        companyRepository.deleteById(id);
        return ResponseEntity.ok("Company deleted successfully!");
    }
}
