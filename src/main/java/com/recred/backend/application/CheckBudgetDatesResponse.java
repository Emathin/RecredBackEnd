package com.recred.backend.application;

import java.util.List;

public record CheckBudgetDatesResponse(
        List<DateBudgetStatus> validationResults
) {
    public record DateBudgetStatus(
            String date,
            boolean blocked,
            String reason
    ) {}
}
