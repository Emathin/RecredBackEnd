package com.recred.backend.domain.models;

import java.time.LocalDate;
import java.util.UUID;

public record HealthyRecommendationPattern(
        UUID studentId,
        LocalDate calculationDate,
        LocalDate startDate,
        LocalDate endDate,
        UUID repeatedProductId,
        String repeatedProductName,
        String repeatedProductCategory,
        UUID buffetId,
        int repeatedDays,
        long totalQuantity,
        UUID suggestedProductId,
        String suggestedProductName,
        String suggestedProductCategory
) {
}
