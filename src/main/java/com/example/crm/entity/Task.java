package com.example.crm.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private LocalDate dueDate;
    private String priority; // "HIGH", "MEDIUM", "LOW"
    private String status = "PENDING"; // "PENDING", "COMPLETED"

    @ManyToOne
    @JoinColumn(name = "executive_id")
    private User executive;

    @ManyToOne
    @JoinColumn(name = "manager_id")
    private User manager;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public User getExecutive() { return executive; }
    public void setExecutive(User executive) { this.executive = executive; }
    public User getManager() { return manager; }
    public void setManager(User manager) { this.manager = manager; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}