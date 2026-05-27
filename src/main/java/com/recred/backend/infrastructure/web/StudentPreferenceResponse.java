package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.repositories.PreferenceAIRepository.PreferenciaDetectada;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;

public record StudentPreferenceResponse(
    String titulo,
    String mensaje,
    String productoId,
    String razonIA
) {
    public static StudentPreferenceResponse fromDomain(PreferenciaDetectada domain) {
        return new StudentPreferenceResponse(
            domain.titulo(),
            domain.mensaje(),
            domain.productoId(),
            domain.razonIA()
        );
    }

    public static StudentPreferenceResponse fromEntity(SugerenciaIAEntity entity) {
        String prodId = entity.getMetadata() != null 
                ? (String) entity.getMetadata().get("productoId") : null;
        String razon = entity.getMetadata() != null 
                ? (String) entity.getMetadata().get("razonIA") : null;
        
        return new StudentPreferenceResponse(
            entity.getTitulo(),
            entity.getMensaje(),
            prodId,
            razon
        );
    }
}
