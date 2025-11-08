package com.example.Agritrack.CompanyNames.Repository;

import com.example.Agritrack.CompanyNames.Model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    boolean existsByShortName(String shortName);
}
