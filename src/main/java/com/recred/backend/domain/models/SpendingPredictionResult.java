package com.recred.backend.domain.models;

import java.util.Optional;

public record SpendingPredictionResult(
        SpendingPrediction spendingPrediction,
        Optional<SpendingPredictionAnalysis> aiAnalysis
) {
}
