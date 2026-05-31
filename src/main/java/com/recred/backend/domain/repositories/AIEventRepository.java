package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Merchant;
import com.recred.backend.domain.models.Product;
import com.recred.backend.domain.models.ProductRecommendationResult;
import com.recred.backend.domain.models.SpendingPredictionResult;

import java.util.UUID;

public interface AIEventRepository {
    void registerPrediction(SpendingPredictionResult result);
    void registerProductChangeRecommendation(ProductRecommendationResult result);
    
    void registerProductAdditionRecommendation(Product product, Merchant merchant,
                                               UUID buffetId);
}
