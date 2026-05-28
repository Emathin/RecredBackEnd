package com.recred.backend.domain.models;

import java.time.LocalDate;
import java.util.List;

public record HealthyRecommendationAnalysisRequest(
        String studentName,
        LocalDate calculationDate,
        LocalDate startDate,
        LocalDate endDate,
        String repeatedProductName,
        String repeatedProductCategory,
        int repeatedDays,
        long totalQuantity,
        List<RecentProductConsumption> consumptionHistory,
        List<HealthyProductOption> availableOptions
) {
}
