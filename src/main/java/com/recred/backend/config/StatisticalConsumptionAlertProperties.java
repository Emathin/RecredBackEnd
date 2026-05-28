package com.recred.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "statistical-alert")
public record StatisticalConsumptionAlertProperties(
        @DefaultValue("true")
        boolean enabled,
        @DefaultValue("0 0 9 * * *")
        String cron,
        @DefaultValue("30")
        int lookbackDays,
        @DefaultValue("3")
        int minimumPurchasesForPattern,
        @DefaultValue("7")
        int patternCooldownDays,
        @DefaultValue("14")
        int healthyRecommendationCooldownDays,
        @DefaultValue("2")
        long minimumProductQuantityForSuggestion
) {
}
