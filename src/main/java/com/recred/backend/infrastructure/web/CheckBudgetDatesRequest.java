package com.recred.backend.infrastructure.web;

import java.util.List;
import java.util.UUID;

public record CheckBudgetDatesRequest(
        List<String> dates,
        List<CartItemRequest> items
) {
    public record CartItemRequest(
            UUID productId,
            int quantity
    ) {}
}
