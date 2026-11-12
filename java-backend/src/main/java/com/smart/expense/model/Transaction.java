package com.smart.expense.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;
    private Double amount;
    private String type;          // "expense" or "income"
    private String category;
    private String paymentMethod; // Cash, UPI, Debit Card, Credit Card, Net Banking
    private String paymentType;   // EXTERNAL or INTERNAL
    private LocalDateTime createdAt = LocalDateTime.now();

    public Transaction() {}

    public Transaction(String description, Double amount, String type, String category, String paymentMethod, String paymentType) {
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.paymentMethod = paymentMethod;
        this.paymentType = paymentType;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

