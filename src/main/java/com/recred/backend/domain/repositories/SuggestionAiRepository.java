package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.PurchaseRecommendationResult;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;

import java.util.List;
import java.util.UUID;

public interface SuggestionAiRepository {
    SugerenciaIAEntity save(UUID eventID, PurchaseRecommendationResult result);

    List<NotificacionEntity> findByTipo(String tomorrow);

    NotificacionEntity findByTipoAndStudentId(String today, UUID studentID);

    com.recred.backend.domain.models.SugerenciaIA getSuggestionByNotificationId(
            UUID notificationId);
}
