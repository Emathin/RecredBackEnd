package com.recred.backend.domain.services;

import com.recred.backend.domain.models.SpendingPredictionAnalysis;
import com.recred.backend.domain.models.SpendingPrediction;
import java.util.Optional;

public interface SpendingPredictionAnalyzer {
    Optional<SpendingPredictionAnalysis> analyze(SpendingPrediction spendingPrediction);
}
