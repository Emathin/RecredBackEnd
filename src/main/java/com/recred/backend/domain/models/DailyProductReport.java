package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.util.UUID;

public record DailyProductReport(
        UUID productId,
        String productName,
        int quantity,
        BigDecimal total
) {
}
