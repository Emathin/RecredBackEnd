package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public record ConsumptionAlertPattern(
        ConsumptionAlertType type,
        UUID studentId,
        LocalDate calculationDate,
        LocalDate startDate,
        LocalDate endDate,
        long purchaseCount,
        BigDecimal totalAmount,
        String mainCategory,
        BigDecimal mainCategoryPercentage,
        UUID suggestedProductId,
        String suggestedProductName
) {

    public Optional<UUID> suggestedProductIdValue() {
        return Optional.ofNullable(suggestedProductId);
    }

    public Optional<String> suggestedProductNameValue() {
        return Optional.ofNullable(suggestedProductName)
                .filter(value -> !value.isBlank());
    }
}
