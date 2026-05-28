package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConsumptionAlertAnalysisRequest(
        ConsumptionAlertType type,
        String studentName,
        LocalDate calculationDate,
        LocalDate startDate,
        LocalDate endDate,
        long purchaseCount,
        BigDecimal totalAmount,
        String mainCategory,
        BigDecimal mainCategoryPercentage,
        String suggestedProductName
) {
}
