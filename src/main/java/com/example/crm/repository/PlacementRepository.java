package com.example.crm.repository;

import com.example.crm.entity.Placement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlacementRepository extends JpaRepository<Placement, Long> {
    List<Placement> findByStudentId(Long studentId);
    List<Placement> findByCompanyId(Long companyId);
}
