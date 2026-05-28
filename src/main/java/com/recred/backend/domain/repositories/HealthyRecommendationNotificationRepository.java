package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.HealthyProductRecommendation;
import java.time.LocalDateTime;
import java.util.UUID;

public interface HealthyRecommendationNotificationRepository {

    boolean hasRecommendationSince(UUID studentId, LocalDateTime since);

    void save(HealthyProductRecommendation recommendation);
}
