package com.example.crm.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "finance_transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Double getBaseAmount() {
		return baseAmount;
	}

	public void setBaseAmount(Double baseAmount) {
		this.baseAmount = baseAmount;
	}

	public Double getVatRate() {
		return vatRate;
	}

	public void setVatRate(Double vatRate) {
		this.vatRate = vatRate;
	}

	public Double getVatAmount() {
		return vatAmount;
	}

	public void setVatAmount(Double vatAmount) {
		this.vatAmount = vatAmount;
	}

	public Double getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(Double totalAmount) {
		this.totalAmount = totalAmount;
	}

	public LocalDateTime getTransactionDate() {
		return transactionDate;
	}

	public void setTransactionDate(LocalDateTime transactionDate) {
		this.transactionDate = transactionDate;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public Double getExchangeRate() {
		return exchangeRate;
	}

	public void setExchangeRate(Double exchangeRate) {
		this.exchangeRate = exchangeRate;
	}

	public User getRecordedBy() {
		return recordedBy;
	}

	public void setRecordedBy(User recordedBy) {
		this.recordedBy = recordedBy;
	}

	// Transaction Type: INCOME or EXPENSE
    private String type; 

    // Financial Category (e.g., Sales, Rent, Utilities, Salaries)
 // Financial Category (e.g., Sales, Rent, Utilities, Salaries, GOSI)
 @Column(name = "category")
 private String category;

    private Double baseAmount; // Amount before VAT
    
    // GCC Standard VAT (e.g., 5% or 15% depending on specific country logic)
    private Double vatRate; 
    
    private Double vatAmount;
    
    private Double totalAmount; // Final amount including VAT

    private LocalDateTime transactionDate;
    
 // Inside Transaction.java
    @Column(name = "currency")
    private String currency; // e.g., "INR", "AED", "BHD"

    @Column(name = "exchange_rate")
    private Double exchangeRate; // Rate relative to your base currency (e.g., INR)

    @ManyToOne
    @JoinColumn(name = "recorded_by")
    private User recordedBy;

    @PrePersist
    protected void onCreate() {
        this.transactionDate = LocalDateTime.now();
        // Automatically calculate totals if not set
        if (vatAmount == null && baseAmount != null && vatRate != null) {
            this.vatAmount = baseAmount * (vatRate / 100);
            this.totalAmount = baseAmount + this.vatAmount;
        }
    }
    
    
    
}