package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Getter
@Setter
@NoArgsConstructor
public class PurchaseRecommendationResult {
    private  UUID alumnoId;
    private  List<PurchaseResume> purchases;
    private  Optional<PurchasePatternAnalysis> aiAnalysis;

    public PurchaseRecommendationResult(
            UUID alumnoId, List<PurchaseResume> purchases,
            Optional<PurchasePatternAnalysis> aiAnalysis) {
        this.alumnoId = alumnoId;
        this.purchases = purchases;
        this.aiAnalysis = aiAnalysis;
    }
}
