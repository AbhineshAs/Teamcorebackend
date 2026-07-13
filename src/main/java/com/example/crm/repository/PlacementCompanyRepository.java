package com.example.crm.repository;

import com.example.crm.entity.PlacementCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlacementCompanyRepository extends JpaRepository<PlacementCompany, Long> {
    PlacementCompany findByCompanyName(String companyName);
}
