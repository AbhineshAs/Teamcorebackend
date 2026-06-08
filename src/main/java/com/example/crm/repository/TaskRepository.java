package com.example.crm.repository;

import com.example.crm.entity.Task;
import com.example.crm.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    // Finds tasks assigned to a specific Executive
    List<Task> findByExecutive(User executive);
    
    // Finds tasks created by a specific Manager
    List<Task> findByManager(User manager);
    
    List<Task> findByUser(User user); // THIS FIXES THE HRCONTROLLER ERROR
// ADD THESE NEW METHODS TO FIX THE ERROR:
    
    /** Counts total tasks assigned to a specific executive */
    long countByExecutive(User executive);

    /** Counts tasks for an executive filtered by status (e.g., "PENDING" or "COMPLETED") */
    long countByExecutiveAndStatus(User executive, String status);
    
 // ADD THIS LINE
    List<Task> findByExecutiveAndStatus(User executive, String status);

    
}