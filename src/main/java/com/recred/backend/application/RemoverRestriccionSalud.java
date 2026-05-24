package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;

import java.util.UUID;

public class RemoverRestriccionSalud {
    private final AlumnoRepository alumnoRepository;

    public RemoverRestriccionSalud(AlumnoRepository alumnoRepository) {
        this.alumnoRepository = alumnoRepository;
    }

    public void ejecutar(UUID alumnoId, UUID clasificacionId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("El alumno no existe."));

        alumno.removerRestriccionSalud(clasificacionId);

        alumnoRepository.save(alumno);
    }
}
