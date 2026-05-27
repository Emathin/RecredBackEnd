package com.recred.backend.domain.services;

import com.recred.backend.domain.models.HealthyRecommendationAnalysisRequest;
import com.recred.backend.domain.models.HealthyRecommendationMessages;
import java.util.Optional;

public interface HealthyRecommendationAnalyzer {

    Optional<HealthyRecommendationMessages> analyze(
            HealthyRecommendationAnalysisRequest request
    );
}
