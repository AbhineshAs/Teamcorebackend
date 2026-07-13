package com.example.crm.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "employee_login_history")
public class EmployeeLoginHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String employeeName;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private String browser;
    private String ipAddress;
    private String location;
    private String duration;
    private String status;
}
