package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.repositories.PreferenceAIRepository.DetectedPreference;
import com.recred.backend.infrastructure.entities.AISuggestionEntity;

public record StudentPreferenceResponse(
    String tittle,
    String message,
    String productID,
    String razonIA
) {
    public static StudentPreferenceResponse fromDomain(DetectedPreference domain) {
        return new StudentPreferenceResponse(
            domain.tittle(),
            domain.message(),
            domain.productoId(),
            domain.razonIA()
        );
    }

    public static StudentPreferenceResponse fromEntity(AISuggestionEntity entity) {
        String prodId = entity.getMetadata() != null 
                ? (String) entity.getMetadata().get("productId") : null;
        String razon = entity.getMetadata() != null 
                ? (String) entity.getMetadata().get("razonIA") : null;
        
        return new StudentPreferenceResponse(
            entity.getTittle(),
            entity.getMessage(),
            prodId,
            razon
        );
    }
}
