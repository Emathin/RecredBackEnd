package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.PriceAlertSuggestion;
import com.recred.backend.domain.models.IaSuggestionType;
import com.recred.backend.infrastructure.entities.AISuggestionEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AISuggestionRepository {
    void save(AISuggestionEntity sugerencia);

    void savePriceAlert(PriceAlertSuggestion suggestion);

    boolean hasRecentPriceAlert(UUID studentId, UUID productId, LocalDateTime since);

    List<AISuggestionEntity> findByAlumnoId(UUID alumnoId);

    List<AISuggestionEntity> findByAlumnoIdAndTipo(UUID alumnoId, IaSuggestionType tipo);

    java.util.Optional<AISuggestionEntity> findById(UUID id);
}
