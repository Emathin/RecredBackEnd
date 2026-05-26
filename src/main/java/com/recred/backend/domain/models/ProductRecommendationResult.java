package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Optional;
@Getter
@Setter
@NoArgsConstructor
public class ProductRecommendationResult {

    private ProductChangeRecommendation productChangeRecommendation;
    private Optional<ProductChangeAnalysis> aiAnalysis;

    public ProductRecommendationResult(ProductChangeRecommendation productChangeRecommendation,
                                       Optional<ProductChangeAnalysis> aiAnalysis) {
        this.productChangeRecommendation = productChangeRecommendation;
        this.aiAnalysis = aiAnalysis;
    }
}
