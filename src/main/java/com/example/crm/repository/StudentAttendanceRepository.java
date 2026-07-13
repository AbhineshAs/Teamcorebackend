package com.example.crm.repository;

import com.example.crm.entity.StudentAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StudentAttendanceRepository extends JpaRepository<StudentAttendance, Long> {
    List<StudentAttendance> findByStudentId(Long studentId);
    List<StudentAttendance> findByBatchId(Long batchId);
    List<StudentAttendance> findByDate(LocalDate date);
    List<StudentAttendance> findByBatchIdAndDate(Long batchId, LocalDate date);
}
