package com.example.crm.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "student_notes")
public class StudentNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(columnDefinition = "TEXT")
    private String noteText;

    private LocalDateTime createdAt = LocalDateTime.now();
    private String author;
}
