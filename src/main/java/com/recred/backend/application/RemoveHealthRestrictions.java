package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;

import java.util.UUID;

public class RemoveHealthRestrictions {
    private final AlumnoRepository alumnoRepository;

    public RemoveHealthRestrictions(AlumnoRepository alumnoRepository) {
        this.alumnoRepository = alumnoRepository;
    }

    public RemoveHealthRestrictionsResponse execute(RemoveHealthRestrictionsRequest request) {
        Alumno alumno = this.findAlumnoById(request.alumnoId());
        alumno.removerRestriccionSalud(request.clasificacionId());
        alumnoRepository.save(alumno);
        return new RemoveHealthRestrictionsResponse();
    }

    private Alumno findAlumnoById(UUID id) {
        return alumnoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El alumno no existe."));
    }
}
