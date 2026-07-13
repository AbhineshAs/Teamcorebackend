package com.example.crm.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "batches")
public class Batch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String batchName;
    private String course;

    @ManyToOne
    @JoinColumn(name = "trainer_id")
    private Trainer trainer;

    private LocalDate startingDate;
    private LocalDate endingDate;
    private String duration;
    
    // Modes: Online, Offline, Hybrid
    private String mode;
    
    // Timing: Morning, Afternoon, Evening
    private String timing;
    
    private Integer seats = 30;
    private Integer availableSeats = 30;
    
    // Status: Upcoming, Running, Completed
    private String status = "Upcoming";
}
