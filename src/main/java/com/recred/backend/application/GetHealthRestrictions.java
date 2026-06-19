package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.repositories.AlumnoRepository;

import java.util.Set;
import java.util.UUID;

public class GetHealthRestrictions {

    private final AlumnoRepository alumnoRepository;

    public GetHealthRestrictions(AlumnoRepository alumnoRepository) {
        this.alumnoRepository = alumnoRepository;
    }

    public Set<ClasificacionSalud> execute(UUID alumnoId) {

        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("El alumno con ID " + alumnoId +
                        " no existe."));

        return alumno.getRestriccionesSalud();
    }
}
