package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.PurchaseRecommendationResult;
import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;

import java.util.List;
import java.util.UUID;

public interface SuggestionAiRepository {
    SugerenciaIAEntity save(UUID eventID, PurchaseRecommendationResult result);

    List<NotificacionEntity> findByTipo(String tomorrow);

    NotificacionEntity findByTipoAndStudentId(String today, UUID studentID);

    List<SugerenciaIA> findTop3ByBuffetIdAndTipo(UUID buffetId, TipoSugerenciaIA tipo);

    com.recred.backend.domain.models.SugerenciaIA getSuggestionByNotificationId(
            UUID notificationId);
}
