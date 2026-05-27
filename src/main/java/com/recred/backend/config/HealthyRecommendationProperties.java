package com.recred.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "healthy-recommendation")
public record HealthyRecommendationProperties(
        @DefaultValue("true")
        boolean enabled,
        @DefaultValue("0 30 9 * * *")
        String cron,
        @DefaultValue("2")
        int lookbackDays,
        @DefaultValue("2")
        int minimumRepeatedDays,
        @DefaultValue("3")
        int cooldownDays,
        @DefaultValue("10")
        int maxHistoryItems,
        @DefaultValue("20")
        int maxAvailableOptions
) {
}
