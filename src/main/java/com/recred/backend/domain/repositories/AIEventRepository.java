package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.ProductRecommendationResult;
import com.recred.backend.domain.models.SpendingPredictionResult;

public interface AIEventRepository {
    void registerPrediction(SpendingPredictionResult result);
    void registerProductChangeRecommendation(ProductRecommendationResult result);
}
