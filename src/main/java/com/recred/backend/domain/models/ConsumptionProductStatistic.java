package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.util.UUID;

public record ConsumptionProductStatistic(
        UUID productId,
        String productName,
        String category,
        Long itemCount,
        Long quantity,
        BigDecimal totalAmount
) {
}
