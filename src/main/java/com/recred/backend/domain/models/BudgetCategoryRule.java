package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.util.UUID;

public class BudgetCategoryRule {
    private UUID id;
    private ProductCategory category;
    private BigDecimal limitAverage;
    private BigDecimal calculatedLimitAmount;
    private boolean active;

    public BudgetCategoryRule() {
        this.active = true;
    }

    public BudgetCategoryRule(UUID id, ProductCategory category,
                              BigDecimal limitAverage, BigDecimal calculatedLimitAmount,
                              boolean active) {
        this.id = id;
        this.category = category;
        this.limitAverage = limitAverage;
        this.calculatedLimitAmount = calculatedLimitAmount;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }

    public ProductCategory getCategory() {
        return category;
    }
    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public BigDecimal getLimitAverage() {
        return limitAverage;
    }
    public void setLimitAverage(BigDecimal
                                        limitAverage) {
        this.limitAverage = limitAverage;
    }

    public BigDecimal getCalculatedLimitAmount() {
        return calculatedLimitAmount;
    }
    public void setCalculatedLimitAmount(BigDecimal
                                                 calculatedLimitAmount) {
        this.calculatedLimitAmount = calculatedLimitAmount;
    }

    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
}