package com.example.crm.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "employee_attendance")
public class EmployeeAttendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;

    private LocalDate date;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private Integer breakTimeMinutes = 0;
    
    // Statuses: PRESENT, LATE, ABSENT, LEAVE
    private String status;
    private Double hoursWorked = 0.0;
    private Boolean lateLogin = false;
    private Boolean earlyLogout = false;
}
