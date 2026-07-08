package com.recred.backend.domain.models;

import java.math.BigDecimal;

public record ConsumptionCategoryStatistic(
        String category,
        Long itemCount,
        Long quantity,
        BigDecimal totalAmount
) {

    public BigDecimal totalAmountOrZero() {
        return totalAmount == null ? BigDecimal.ZERO : totalAmount;
    }
}
