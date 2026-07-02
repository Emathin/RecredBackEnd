package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.repositories.AlumnoRepository;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class GetHealthRestrictions {

    private final AlumnoRepository alumnoRepository;

    public GetHealthRestrictions(AlumnoRepository alumnoRepository) {
        this.alumnoRepository = alumnoRepository;
    }

    public Set<GetHealthRestrictionsResponse> ejecutar(GetHealthRestrictionsRequest request) {
        Alumno alumno = this.findAlumnoById(request.alumnoId());
        return this.mapToResponse(alumno.getRestriccionesSalud());
    }

    private Alumno findAlumnoById(UUID alumnoId) {
        return alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El alumno con ID " + alumnoId + " no existe."));
    }

    private Set<GetHealthRestrictionsResponse> mapToResponse(
            Set<ClasificacionSalud> restricciones) {
        return restricciones.stream()
                .map(this::toResponse)
                .collect(Collectors.toSet());
    }

    private GetHealthRestrictionsResponse toResponse(ClasificacionSalud clasificacion) {
        return new GetHealthRestrictionsResponse(
                clasificacion.getId(),
                clasificacion.getDescripcion(),
                clasificacion.getActivo()
        );
    }
}

