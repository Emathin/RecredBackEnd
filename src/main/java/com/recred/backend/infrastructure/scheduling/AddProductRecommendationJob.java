package com.recred.backend.infrastructure.scheduling;

import com.recred.backend.application.AddProductRecommendation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "add-product-recommendation.enabled"
        , havingValue = "true", matchIfMissing = true)
public class AddProductRecommendationJob {
    private final AddProductRecommendation addProductRecommendation;

    public AddProductRecommendationJob(AddProductRecommendation addProductRecommendation) {
        this.addProductRecommendation = addProductRecommendation;
    }

    @Scheduled(cron = "0 13 20 * * *")
    public void run() {
        addProductRecommendation.execute();
    }
}
