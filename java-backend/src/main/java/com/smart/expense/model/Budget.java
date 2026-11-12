package com.smart.expense.model;

import jakarta.persistence.*;

@Entity
@Table(name = "budgets")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String category;
    private Double monthlyLimit;
    private String periodType; // WEEKLY, MONTHLY, ANNUAL

    public Budget() {}

    public Budget(String category, Double monthlyLimit, String periodType) {
        this.category = category;
        this.monthlyLimit = monthlyLimit;
        this.periodType = periodType;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getMonthlyLimit() { return monthlyLimit; }
    public void setMonthlyLimit(Double monthlyLimit) { this.monthlyLimit = monthlyLimit; }

    public String getPeriodType() { return periodType; }
    public void setPeriodType(String periodType) { this.periodType = periodType; }
}

