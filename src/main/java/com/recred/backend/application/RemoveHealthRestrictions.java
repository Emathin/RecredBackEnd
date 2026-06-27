package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;

public class RemoveHealthRestrictions {
    private final AlumnoRepository alumnoRepository;

    public RemoveHealthRestrictions(AlumnoRepository alumnoRepository) {
        this.alumnoRepository = alumnoRepository;
    }

    public void execute(RemoveHealthRestrictionsRequest request) {
        Alumno alumno = alumnoRepository.findById(request.alumnoId())
                .orElseThrow(() -> new IllegalArgumentException("El alumno no existe."));

        alumno.removerRestriccionSalud(request.clasificacionId());

        alumnoRepository.save(alumno);
    }
}

