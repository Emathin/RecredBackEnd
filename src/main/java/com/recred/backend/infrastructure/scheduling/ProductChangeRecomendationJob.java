package com.recred.backend.infrastructure.scheduling;

import com.recred.backend.application.GenerateProductChangeRecommendation;
import org.springframework.scheduling.annotation.Scheduled;

public class ProductChangeRecomendationJob {
    private final GenerateProductChangeRecommendation generateProductChangeRecommendation;


    public ProductChangeRecomendationJob(
             GenerateProductChangeRecommendation generateProductChangeRecommendation

    ) {
        this.generateProductChangeRecommendation = generateProductChangeRecommendation;
    }

    @Scheduled(cron = "${statistical-alert.cron:0 0 9 * * *}")
    public void run() {

        generateProductChangeRecommendation.execute();
    }
}
