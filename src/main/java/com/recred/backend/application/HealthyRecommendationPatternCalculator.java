package com.recred.backend.application;

import com.recred.backend.domain.models.HealthyRepeatedProductPattern;
import com.recred.backend.domain.models.RecentProductConsumption;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

class HealthyRecommendationPatternCalculator {

    private static final int MINIMUM_REPEATED_DAYS = 1;

    List<HealthyRepeatedProductPattern> calculate(
            List<RecentProductConsumption> consumptions,
            int configuredMinimumRepeatedDays
    ) {
        int minimumRepeatedDays = minimumRepeatedDays(configuredMinimumRepeatedDays);
        return groupByProduct(consumptions).values()
                .stream()
                .map(this::toPattern)
                .filter(pattern -> pattern.hasAtLeastRepeatedDays(minimumRepeatedDays))
                .sorted(patternComparator())
                .toList();
    }

    private int minimumRepeatedDays(int configuredMinimumRepeatedDays) {
        return Math.max(configuredMinimumRepeatedDays, MINIMUM_REPEATED_DAYS);
    }

    private Map<ProductPatternKey, List<RecentProductConsumption>> groupByProduct(
            List<RecentProductConsumption> consumptions
    ) {
        return consumptions.stream().collect(Collectors.groupingBy(ProductPatternKey::from));
    }

    private HealthyRepeatedProductPattern toPattern(
            List<RecentProductConsumption> consumptions
    ) {
        RecentProductConsumption firstConsumption = consumptions.get(0);
        return new HealthyRepeatedProductPattern(
                firstConsumption.productId(),
                firstConsumption.productName(),
                firstConsumption.categoryName(),
                firstConsumption.buffetId(),
                repeatedDays(consumptions),
                totalQuantity(consumptions),
                latestPurchaseDateTime(consumptions)
        );
    }

    private int repeatedDays(List<RecentProductConsumption> consumptions) {
        return consumptions.stream()
                .map(RecentProductConsumption::purchaseDate)
                .collect(Collectors.toSet())
                .size();
    }

    private long totalQuantity(List<RecentProductConsumption> consumptions) {
        return consumptions.stream()
                .map(RecentProductConsumption::quantityOrZero)
                .mapToLong(Integer::longValue)
                .sum();
    }

    private LocalDateTime latestPurchaseDateTime(List<RecentProductConsumption> consumptions) {
        return consumptions.stream()
                .map(RecentProductConsumption::purchaseDateTime)
                .max(Comparator.naturalOrder())
                .orElse(LocalDateTime.MIN);
    }

    private Comparator<HealthyRepeatedProductPattern> patternComparator() {
        return Comparator.comparing(HealthyRepeatedProductPattern::repeatedDays)
                .thenComparing(HealthyRepeatedProductPattern::totalQuantity)
                .thenComparing(HealthyRepeatedProductPattern::latestPurchaseDateTime)
                .reversed();
    }

    private record ProductPatternKey(UUID productId, UUID buffetId) {

        private static ProductPatternKey from(RecentProductConsumption consumption) {
            return new ProductPatternKey(consumption.productId(), consumption.buffetId());
        }
    }
}
