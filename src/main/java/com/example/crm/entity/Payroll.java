package com.example.crm.entity;

import java.time.LocalDateTime;
// REMOVE: import org.springframework.data.annotation.Id;
import jakarta.persistence.Id; // Use this one
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "payroll")
public class Payroll {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private Integer month;
    private Integer year;

    private Double basicSalary;
    public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
	public Integer getMonth() {
		return month;
	}
	public void setMonth(Integer month) {
		this.month = month;
	}
	public Integer getYear() {
		return year;
	}
	public void setYear(Integer year) {
		this.year = year;
	}
	public Double getBasicSalary() {
		return basicSalary;
	}
	public void setBasicSalary(Double basicSalary) {
		this.basicSalary = basicSalary;
	}
	public Double getTotalAllowances() {
		return totalAllowances;
	}
	public void setTotalAllowances(Double totalAllowances) {
		this.totalAllowances = totalAllowances;
	}
	public Double getGosiDeduction() {
		return gosiDeduction;
	}
	public void setGosiDeduction(Double gosiDeduction) {
		this.gosiDeduction = gosiDeduction;
	}
	public Double getLopDeduction() {
		return lopDeduction;
	}
	public void setLopDeduction(Double lopDeduction) {
		this.lopDeduction = lopDeduction;
	}
	public Double getOtherDeductions() {
		return otherDeductions;
	}
	public void setOtherDeductions(Double otherDeductions) {
		this.otherDeductions = otherDeductions;
	}
	public Double getNetSalary() {
		return netSalary;
	}
	public void setNetSalary(Double netSalary) {
		this.netSalary = netSalary;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public LocalDateTime getProcessedAt() {
		return processedAt;
	}
	public void setProcessedAt(LocalDateTime processedAt) {
		this.processedAt = processedAt;
	}
	private Double totalAllowances;
    
    private Double gosiDeduction;
    private Double lopDeduction; 
    private Double otherDeductions;

    private Double netSalary;
    private String status; 
    private LocalDateTime processedAt;

    // Getters and Setters...
}