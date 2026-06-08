package com.example.crm.repository;

import com.example.crm.entity.Lead;
import com.example.crm.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {
    boolean existsByPhoneNumberOrEmail(String phoneNumber, String email);
    List<Lead> findByUser(User user);

    // Fetch leads for a Manager's entire team
    List<Lead> findByUserIn(List<User> users);
}