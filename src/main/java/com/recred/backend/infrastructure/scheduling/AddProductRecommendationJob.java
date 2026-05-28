package com.recred.backend.infrastructure.scheduling;

import com.recred.backend.application.AddProductRecommendation;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AddProductRecommendationJob {
    private final AddProductRecommendation addProductRecommendation;

    public AddProductRecommendationJob(AddProductRecommendation addProductRecommendation) {
        this.addProductRecommendation = addProductRecommendation;
    }

    @Scheduled(cron = "${product-recommendation.cron:0 30 12 * * *}")
    public void run() {
        addProductRecommendation.execute();
    }
}
