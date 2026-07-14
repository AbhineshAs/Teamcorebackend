package com.example.crm.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "students")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String studentId;

    private String name;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String photo;

    private String phone;

    @Column(nullable = true)
    private String email;

    private String address;
    private String college;
    private String qualification;
    private String coursePurchased;
    private Double courseFees = 0.0;
    private Double paidAmount = 0.0;
    private Double balance = 0.0;
    private LocalDate joiningDate;
    private String salesExecutive;
    
    // Status: PENDING_VERIFICATION, DOCUMENTS_PENDING, BATCH_NOT_ASSIGNED, BATCH_ASSIGNED, STARTED, COMPLETED, PLACED
    private String status = "PENDING_VERIFICATION";

    private Double rating;
    private String mockScore;
    private String projectGrade;

    @ManyToOne
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @ManyToOne
    @JoinColumn(name = "trainer_id")
    private Trainer trainer;
}
