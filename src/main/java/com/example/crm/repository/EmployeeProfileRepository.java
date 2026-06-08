package com.example.crm.repository;

import com.example.crm.entity.EmployeeProfile;
import com.example.crm.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfile, Long> {
    
    // This allows you to find the salary/leave profile for a specific user
    Optional<EmployeeProfile> findByUser(User user);
}