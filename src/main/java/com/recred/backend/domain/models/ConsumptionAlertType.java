package com.recred.backend.domain.models;

public enum ConsumptionAlertType {
    STATISTICAL_PATTERN,
    HEALTHY_RECOMMENDATION;

    public boolean isStatisticalPattern() {
        return this == STATISTICAL_PATTERN;
    }

    public boolean isHealthyRecommendation() {
        return this == HEALTHY_RECOMMENDATION;
    }

    public static ConsumptionAlertType fromPurchaseCount(
            long purchaseCount,
            int minimumPurchasesForPattern
    ) {
        if (purchaseCount >= minimumPurchasesForPattern) {
            return STATISTICAL_PATTERN;
        }

        return HEALTHY_RECOMMENDATION;
    }
}
