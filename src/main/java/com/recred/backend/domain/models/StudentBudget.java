package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StudentBudget {
    private UUID id;
    private UUID studentId;
    private BigDecimal limitAmount;
    private PeriodBudget period;
    private LocalDate startDate;
    private boolean active;
    private List<BudgetCategoryRule> categoryRules;

    public StudentBudget() {
        this.categoryRules = new ArrayList<>();
        this.active = true;
    }

    public StudentBudget(UUID id, UUID studentId, BigDecimal limitAmount,
                         PeriodBudget period, LocalDate startDate,
                         boolean active, List<BudgetCategoryRule> categoryRules) {
        this.id = id;
        this.studentId = studentId;
        this.limitAmount = limitAmount;
        this.period = period;
        this.startDate = startDate;
        this.active = active;
        this.categoryRules = categoryRules != null ? categoryRules : new ArrayList<>();
    }

    public boolean isCurrent(LocalDate currentDate) {
        return active && !currentDate.isBefore(startDate);
    }

    public void validatePercentage() {
        if (this.categoryRules == null || this.categoryRules.isEmpty()) {
            return;
        }

        BigDecimal sumOfPercentages = this.categoryRules.stream()
                .filter(BudgetCategoryRule::isActive)
                .map(r -> r.getLimitAverage() != null ?
                        r.getLimitAverage() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sumOfPercentages.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("La suma de los porcentajes " +
                    "no puede superar el 100%. Total enviado: " + sumOfPercentages + "%");
        }
    }

    public void recalculateRulesBudgets() {
        if (this.categoryRules == null || this.categoryRules.
                isEmpty() || this.limitAmount == null) {
            return;
        }

        for (BudgetCategoryRule regla : this.categoryRules) {
            if (regla.getLimitAverage() != null && regla.isActive()) {
                BigDecimal calculatedAmount = this.limitAmount
                        .multiply(regla.getLimitAverage())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                regla.setCalculatedLimitAmount(calculatedAmount);
            }
        }
    }

    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }
    public UUID getStudentId() {
        return studentId;
    }
    public void setStudentId(UUID studentId) {
        this.studentId = studentId;
    }
    public BigDecimal getLimitAmount() {
        return limitAmount;
    }
    public void setLimitAmount(BigDecimal
                                       limitAmount) {
        this.limitAmount = limitAmount;
    }
    public PeriodBudget getPeriod() {
        return period;
    }
    public void setPeriod(PeriodBudget period) {
        this.period = period;
    }
    public LocalDate getStartDate() {
        return startDate;
    }
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
    public List<BudgetCategoryRule> getCategoryRules() {
        return categoryRules;
    }
    public void setCategoryRules(List<BudgetCategoryRule> categoryRules) {
        this.categoryRules = categoryRules;
    }
}