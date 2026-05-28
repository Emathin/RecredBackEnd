package com.recred.backend.domain.models;

import java.math.BigDecimal;

public record ConsumptionCategoryStatistic(
        String category,
        Long itemCount,
        Long quantity,
        BigDecimal totalAmount
) {
}
