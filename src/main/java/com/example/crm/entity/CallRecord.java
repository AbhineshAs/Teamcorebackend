package com.example.crm.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "call_records")
public class CallRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName;
    private String customerPhone;
    private String direction; // "INBOUND" or "OUTBOUND"
    private LocalDateTime startTime;
    private Integer durationSeconds;
    private String status; // "ANSWERED", "MISSED", "BUSY", "VOICEMAIL"
    private String recordingUrl;
    
    @Column(length = 1000)
    private String transcription;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User agent;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRecordingUrl() { return recordingUrl; }
    public void setRecordingUrl(String recordingUrl) { this.recordingUrl = recordingUrl; }

    public String getTranscription() { return transcription; }
    public void setTranscription(String transcription) { this.transcription = transcription; }

    public User getAgent() { return agent; }
    public void setAgent(User agent) { this.agent = agent; }

    private String simUsed;

    public String getSimUsed() { return simUsed; }
    public void setSimUsed(String simUsed) { this.simUsed = simUsed; }
}
