package com.recred.backend.application;

import java.util.List;
import java.util.UUID;

public record CheckBudgetDatesRequest(
        UUID studentId,
        List<String> dates,
        List<CartItemRequest> items
) {
    public CheckBudgetDatesRequest(
            List<String> dates,
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
