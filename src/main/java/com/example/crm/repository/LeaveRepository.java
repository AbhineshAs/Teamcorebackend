package com.example.crm.repository;

import com.example.crm.entity.LeaveRequest;
import com.example.crm.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRepository extends JpaRepository<LeaveRequest, Long> {

    // Fetch all leave history for a specific employee
    List<LeaveRequest> findByUserOrderByCreatedAtDesc(User user);

    // Fetch requests by status (e.g., used by Admin to see all PENDING requests)
    List<LeaveRequest> findByStatusOrderByCreatedAtDesc(String status);

    // Fetch specific user requests by status
    List<LeaveRequest> findByUserAndStatus(User user, String status);
    
    // Fetch all requests for a manager's team (Optional for Step 3)
    List<LeaveRequest> findByUserInOrderByCreatedAtDesc(List<User> team);
    
 // --- NEW: Duplicate Date Prevention Check ---
    // Checks if any leave exists for this user that overlaps with new dates (excludes REJECTED)
    @Query("SELECT COUNT(l) > 0 FROM LeaveRequest l WHERE l.user = :user " +
           "AND l.status != 'REJECTED' " +
           "AND (:startDate <= l.endDate AND :endDate >= l.startDate)")
    boolean existsOverlappingLeave(@Param("user") User user, 
                                   @Param("startDate") LocalDate startDate, 
                                   @Param("endDate") LocalDate endDate);
    
 // Add this line to fix the error
    List<LeaveRequest> findByStatus(String status);
}