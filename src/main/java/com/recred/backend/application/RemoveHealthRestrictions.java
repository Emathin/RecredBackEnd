package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;

import java.util.UUID;

public class RemoveHealthRestrictions {
    private final AlumnoRepository alumnoRepository;

    public RemoveHealthRestrictions(AlumnoRepository alumnoRepository) {
        this.alumnoRepository = alumnoRepository;
    }

    public void execute(UUID alumnoId, UUID clasificacionId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("El alumno no existe."));

        alumno.removeHealthRestriction(clasificacionId);

        alumnoRepository.save(alumno);
    }
}
