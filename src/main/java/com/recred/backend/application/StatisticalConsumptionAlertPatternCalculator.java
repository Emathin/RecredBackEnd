package com.recred.backend.application;

import com.recred.backend.domain.models.ConsumptionAlertCandidate;
import com.recred.backend.domain.models.ConsumptionAlertPattern;
import com.recred.backend.domain.models.ConsumptionAlertType;
import com.recred.backend.domain.models.ConsumptionCategoryStatistic;
import com.recred.backend.domain.models.ConsumptionProductStatistic;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

class StatisticalConsumptionAlertPatternCalculator {

    private static final int PERCENTAGE_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");

    ConsumptionAlertPattern calculate(
            ConsumptionAlertCandidate candidate,
            ConsumptionAlertType alertType,
            LocalDate calculationDate,
            AlertDateWindow dateWindow,
            long purchaseCount,
            List<ConsumptionCategoryStatistic> categoryStatistics,
            Optional<ConsumptionProductStatistic> suggestedProduct
    ) {
        if (alertType.isHealthyRecommendation()) {
            return healthyRecommendationPattern(
                    candidate,
                    calculationDate,
                    dateWindow,
                    purchaseCount
            );
        }

        return statisticalPattern(
                candidate,
                alertType,
                calculationDate,
                dateWindow,
                purchaseCount,
                categoryStatistics,
                suggestedProduct
        );
    }

    private ConsumptionAlertPattern healthyRecommendationPattern(
            ConsumptionAlertCandidate candidate,
            LocalDate calculationDate,
            AlertDateWindow dateWindow,
            long purchaseCount
    ) {
        return new ConsumptionAlertPattern(
                ConsumptionAlertType.HEALTHY_RECOMMENDATION,
                candidate.studentId(),
                calculationDate,
                dateWindow.startDate(),
                dateWindow.endDate(),
                purchaseCount,
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                null,
                null
        );
    }

    private ConsumptionAlertPattern statisticalPattern(
            ConsumptionAlertCandidate candidate,
            ConsumptionAlertType alertType,
            LocalDate calculationDate,
            AlertDateWindow dateWindow,
            long purchaseCount,
            List<ConsumptionCategoryStatistic> categoryStatistics,
            Optional<ConsumptionProductStatistic> suggestedProduct
    ) {
        ConsumptionCategoryStatistic mainCategory = mainCategory(categoryStatistics);
        BigDecimal totalAmount = totalAmount(categoryStatistics);
        return new ConsumptionAlertPattern(
                alertType,
                candidate.studentId(),
                calculationDate,
                dateWindow.startDate(),
                dateWindow.endDate(),
                purchaseCount,
                totalAmount,
                mainCategory == null ? null : mainCategory.category(),
                categoryPercentage(mainCategory, totalAmount),
                suggestedProduct.map(ConsumptionProductStatistic::productId).orElse(null),
                suggestedProduct.map(ConsumptionProductStatistic::productName).orElse(null)
        );
    }

    private ConsumptionCategoryStatistic mainCategory(
            List<ConsumptionCategoryStatistic> statistics
    ) {
        return statistics.stream()
                .max(Comparator.comparing(ConsumptionCategoryStatistic::totalAmountOrZero))
                .orElse(null);
    }

    private BigDecimal totalAmount(List<ConsumptionCategoryStatistic> statistics) {
        return statistics.stream()
                .map(ConsumptionCategoryStatistic::totalAmountOrZero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal categoryPercentage(
            ConsumptionCategoryStatistic mainCategory,
            BigDecimal totalAmount
    ) {
        if (mainCategory == null || totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return mainCategory.totalAmountOrZero()
                .multiply(ONE_HUNDRED)
                .divide(totalAmount, PERCENTAGE_SCALE, RoundingMode.HALF_UP);
    }

    record AlertDateWindow(
            LocalDate startDate,
            LocalDate endDate
    ) {
    }
}
