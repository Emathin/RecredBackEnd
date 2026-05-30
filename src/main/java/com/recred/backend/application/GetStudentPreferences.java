package com.recred.backend.application;

import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import java.util.List;
import java.util.UUID;

public class GetStudentPreferences {

    private final SugerenciaIARepository sugerenciaIARepository;

    public GetStudentPreferences(SugerenciaIARepository sugerenciaIARepository) {
        this.sugerenciaIARepository = sugerenciaIARepository;
    }

    public List<SugerenciaIAEntity> execute(
            UUID alumnoId, boolean onlyLast, TipoSugerenciaIA tipo) {
        List<SugerenciaIAEntity> preferences;

        if (tipo != null) {
            preferences = sugerenciaIARepository.findByAlumnoIdAndTipo(alumnoId, tipo);
        } else {
            preferences = sugerenciaIARepository.findByAlumnoId(alumnoId);
        }
        
        if (onlyLast && !preferences.isEmpty()) {
            return List.of(preferences.get(0));
        }
        
        return preferences;
    }
}
