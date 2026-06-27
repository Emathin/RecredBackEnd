package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;

import java.util.Set;
import java.util.stream.Collectors;

public class GetHealthRestrictions {

    private final AlumnoRepository alumnoRepository;

    public GetHealthRestrictions(AlumnoRepository alumnoRepository) {
        this.alumnoRepository = alumnoRepository;
    }

    public Set<GetHealthRestrictionsResponse> ejecutar(GetHealthRestrictionsRequest request) {

        Alumno alumno = alumnoRepository.findById(request.alumnoId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "El alumno con ID " + request.alumnoId() + " no existe."));

        return alumno.getRestriccionesSalud().stream()
                .map(clasificacion -> new GetHealthRestrictionsResponse(
                        clasificacion.getId(),
                        clasificacion.getDescripcion(),
                        clasificacion.getActivo()
                ))
                .collect(Collectors.toSet());
    }
}

