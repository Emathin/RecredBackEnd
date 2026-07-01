package com.recred.backend.infrastructure.scheduling;

import com.recred.backend.application.GenerateProductChangeRecommendation;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProductChangeRecomendationJob {
    private final GenerateProductChangeRecommendation generateProductChangeRecommendation;


    public ProductChangeRecomendationJob(
             GenerateProductChangeRecommendation generateProductChangeRecommendation

    ) {
        this.generateProductChangeRecommendation = generateProductChangeRecommendation;
    }

    @Scheduled(cron = "0 00 23 1,15 * *")
    public void run() {
        generateProductChangeRecommendation.execute();
    }
}
