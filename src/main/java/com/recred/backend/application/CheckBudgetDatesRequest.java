package com.recred.backend.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CheckBudgetDatesRequest(
        UUID studentId,
        List<LocalDate> dates,
        List<CartItemRequest> items
) {
    public CheckBudgetDatesRequest(
            List<LocalDate> dates,
            List<CartItemRequest> items
    ) {
        this(null, dates, items);
    }

    public CheckBudgetDatesRequest withStudentId(UUID studentId) {
        return new CheckBudgetDatesRequest(studentId, dates, items);
    }

    public record CartItemRequest(
            UUID productId,
            int quantity
    ) {}
}
