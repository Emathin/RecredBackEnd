package com.recred.backend.infrastructure.scheduling;

import com.recred.backend.application.GeneratePurchaseRecommendation;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AddPurchaseRecommendationJob {

    private final GeneratePurchaseRecommendation generatePurchaseRecommendation;

    public AddPurchaseRecommendationJob(
            GeneratePurchaseRecommendation generatePurchaseRecommendation) {
        this.generatePurchaseRecommendation = generatePurchaseRecommendation;
    }

    @Scheduled(cron = "${product-recommendation.cron:0 30 12 * * *}")
    public void run() {
        generatePurchaseRecommendation.execute();
    }
}
