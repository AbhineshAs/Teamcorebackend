package com.example.crm.repository;

import com.example.crm.entity.AttendanceLog;
import com.example.crm.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<AttendanceLog, Long> {
    // Returns a List to handle potential duplicates safely
    List<AttendanceLog> findByUserAndDate(User user, LocalDate date);
    
    List<AttendanceLog> findAllByOrderByCheckInDesc();

    List<AttendanceLog> findByUser(User user);
}