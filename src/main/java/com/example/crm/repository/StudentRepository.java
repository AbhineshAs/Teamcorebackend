package com.example.crm.repository;

import com.example.crm.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Student findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByStudentId(String studentId);
    List<Student> findByBatchId(Long batchId);
    List<Student> findByTrainerId(Long trainerId);
}
