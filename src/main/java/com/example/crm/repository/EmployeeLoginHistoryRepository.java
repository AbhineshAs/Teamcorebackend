package com.example.crm.repository;

import com.example.crm.entity.EmployeeLoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeLoginHistoryRepository extends JpaRepository<EmployeeLoginHistory, Long> {
}
