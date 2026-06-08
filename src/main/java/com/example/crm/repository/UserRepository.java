package com.example.crm.repository;

import com.example.crm.entity.User;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByRole(String role);
    User findByEmail(String email);
    boolean existsByEmail(String email);
    // NEW: Find all Executives assigned to a specific Manager
    List<User> findByManager(User manager);
}
