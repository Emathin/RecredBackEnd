package com.recred.backend.domain.services;

import com.recred.backend.domain.models.ProductChangeAnalysis;
import com.recred.backend.domain.models.ProductChangeRecommendation;

import java.util.Optional;

public interface ProductChangeAnalyzer {
    Optional<ProductChangeAnalysis> analyze(
            ProductChangeRecommendation ProductChangeRecommendation);
}
