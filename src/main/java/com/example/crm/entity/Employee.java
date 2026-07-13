package com.example.crm.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "employees")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String employeeId;

    private String name;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String photo;

    private String gender;
    private LocalDate dob;

    @Column(unique = true, nullable = false)
    private String email;

    private String phone;
    private String address;
    private String department;
    private String role;
    private LocalDate joiningDate;
    private Double salary;
    private String qualification;
    private Integer experience;
    private String skills;
    private String status; // Active, Inactive, On Leave, Resigned
    private String username;
    private String password;
    private String emergencyContact;
    private String bankDetails;
    private String aadharNumber;
    private String panNumber;
    private String documentsUpload;
}
