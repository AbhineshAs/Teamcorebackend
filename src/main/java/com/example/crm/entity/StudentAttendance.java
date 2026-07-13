package com.example.crm.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "attendance")
public class StudentAttendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "batch_id")
    private Batch batch;

    private LocalDate date;
    
    // Status: PRESENT, ABSENT, LEAVE
    private String status;
}
