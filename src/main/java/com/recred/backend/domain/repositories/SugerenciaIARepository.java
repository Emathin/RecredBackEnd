package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.PriceAlertSuggestion;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SugerenciaIARepository {
    void guardar(SugerenciaIAEntity sugerencia);

    void savePriceAlert(PriceAlertSuggestion suggestion);

    boolean hasRecentPriceAlert(UUID studentId, UUID productId, LocalDateTime since);

    List<SugerenciaIAEntity> findByAlumnoId(UUID alumnoId);

    List<SugerenciaIAEntity> findByAlumnoIdAndTipo(UUID alumnoId, TipoSugerenciaIA tipo);

    List<UUID> findProductIdsByAlumnoIdAndTipo(UUID alumnoId, TipoSugerenciaIA tipo);

    java.util.Optional<SugerenciaIAEntity> findById(UUID id);

}
