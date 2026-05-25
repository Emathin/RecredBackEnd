package com.recred.backend.domain.implementation;

import com.recred.backend.domain.models.SpendingPredictionAnalysis;
import com.recred.backend.domain.models.SpendingPrediction;
import java.util.Optional;

public interface SpendingPredictionAnalyzer {
    Optional<SpendingPredictionAnalysis> analyze(SpendingPrediction spendingPrediction);
}
