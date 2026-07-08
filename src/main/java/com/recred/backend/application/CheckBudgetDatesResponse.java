package com.recred.backend.application;

import java.time.LocalDate;
import java.util.List;

public record CheckBudgetDatesResponse(
        List<DateBudgetStatus> validationResults
) {
    public record DateBudgetStatus(
            LocalDate date,
            boolean blocked,
            String reason
    ) {}
}
