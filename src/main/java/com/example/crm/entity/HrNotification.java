package com.example.crm.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "notifications")
public class HrNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;
    
    // Types: NEW_STUDENT, SALES_CLOSED, PAYMENT_COMPLETED, BATCH_STARTED, TRAINER_ASSIGNED, PLACEMENT_SCHEDULED, INTERVIEW_REMINDER, DOCUMENT_PENDING
    private String type;
    
    private LocalDateTime createdAt = LocalDateTime.now();
    private Boolean isRead = false;
}
