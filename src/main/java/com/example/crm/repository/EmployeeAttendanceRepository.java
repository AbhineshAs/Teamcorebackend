package com.example.crm.repository;

import com.example.crm.entity.Employee;
import com.example.crm.entity.EmployeeAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeAttendanceRepository extends JpaRepository<EmployeeAttendance, Long> {
    List<EmployeeAttendance> findByEmployeeId(Long employeeId);
    List<EmployeeAttendance> findByDate(LocalDate date);
    Optional<EmployeeAttendance> findByEmployeeAndDate(Employee employee, LocalDate date);
}
