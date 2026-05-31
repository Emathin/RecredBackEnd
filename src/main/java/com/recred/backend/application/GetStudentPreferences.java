package com.recred.backend.application;

import com.recred.backend.domain.models.IaSuggestionType;
import com.recred.backend.domain.repositories.AISuggestionRepository;
import com.recred.backend.infrastructure.entities.AISuggestionEntity;
import java.util.List;
import java.util.UUID;

public class GetStudentPreferences {

    private final AISuggestionRepository AISuggestionRepository;

    public GetStudentPreferences(AISuggestionRepository AISuggestionRepository) {
        this.AISuggestionRepository = AISuggestionRepository;
    }

    public List<AISuggestionEntity> execute(
            UUID alumnoId, boolean onlyLast, IaSuggestionType tipo) {
        List<AISuggestionEntity> preferences;

        if (tipo != null) {
            preferences = AISuggestionRepository.findByAlumnoIdAndTipo(alumnoId, tipo);
        } else {
            preferences = AISuggestionRepository.findByAlumnoId(alumnoId);
        }
        
        if (onlyLast && !preferences.isEmpty()) {
            return List.of(preferences.get(0));
        }
        
        return preferences;
    }
}
