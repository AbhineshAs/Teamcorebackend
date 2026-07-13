package com.example.crm.repository;

import com.example.crm.entity.StudentPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentPaymentRepository extends JpaRepository<StudentPayment, Long> {
    List<StudentPayment> findByStudentId(Long studentId);
}
