package com.example.crm.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "trainers")
public class Trainer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String phone;
    private Integer experience;
    private String courses; // comma-separated courses taught
    private String availableTime; // Available Time slots
    private String currentBatch;
    private Integer studentCount = 0;
    private Double performance = 0.0;
    private String role; // TRAINER or TECH_LEAD
}
