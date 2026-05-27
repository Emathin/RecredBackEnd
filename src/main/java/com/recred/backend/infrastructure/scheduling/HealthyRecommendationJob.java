package com.recred.backend.infrastructure.scheduling;

import com.recred.backend.application.GenerateHealthyProductRecommendations;
import com.recred.backend.config.HealthyRecommendationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HealthyRecommendationJob {

    private final GenerateHealthyProductRecommendations generateHealthyProductRecommendations;
    private final HealthyRecommendationProperties properties;

    public HealthyRecommendationJob(
            GenerateHealthyProductRecommendations generateHealthyProductRecommendations,
            HealthyRecommendationProperties properties
    ) {
        this.generateHealthyProductRecommendations = generateHealthyProductRecommendations;
        this.properties = properties;
    }

    @Scheduled(cron = "${healthy-recommendation.cron:0 30 9 * * *}")
    public void run() {
        if (!properties.enabled()) {
            return;
        }

        generateHealthyProductRecommendations.execute();
    }
}
