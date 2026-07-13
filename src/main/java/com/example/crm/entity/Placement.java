package com.example.crm.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "placements")
public class Placement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private PlacementCompany company;

    private String position;
    private Double salary = 0.0; // Monthly or CTC
    private LocalDate interviewDate;
    private String interviewTime;
    private String interviewRound;
    
    // Statuses: Preparing, Interview Scheduled, Interview Completed, Selected, Rejected, Joined, Placed
    private String status = "Preparing";

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String offerLetter;

    private LocalDate joiningDate;
    private Double packageAmount = 0.0; // CTC in LPA or numeric
    private String remarks;
}
