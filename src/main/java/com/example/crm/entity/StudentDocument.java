package com.example.crm.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "student_documents")
public class StudentDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    private String documentType; // Aadhar, PAN, Degree Certificate, Mark Lists, Passport Photo, Resume, ID Proof

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String documentData; // Base64 encoded file data

    private String status = "Pending"; // Verified, Rejected, Pending
    private String remarks;
}
