package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.ProductRecommendationResult;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.SpendingPredictionResult;

import java.util.UUID;

public interface AIEventRepository {
    void registerPrediction(SpendingPredictionResult result);
    void registerProductChangeRecommendation(ProductRecommendationResult result);
    
    void registerProductAdditionRecommendation(Producto product, Kiosquero kiosquero,
                                               UUID buffetId);
}
