package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.PurchaseRecommendationResult;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;

import java.util.UUID;

public interface PurchaseRecommendationNotificationRepository {

    void save(PurchaseRecommendationResult recommendation,
              SugerenciaIAEntity suggestion, UUID userId);
}
