package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.SpendingPredictionResult;

public interface AIEventRepository {
    void registerPrediction(SpendingPredictionResult result);
}
