package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.repositories.AlumnoRepository;

import java.util.Set;
import java.util.UUID;

public class ObtenerRestriccionesSalud {

    private final AlumnoRepository alumnoRepository;

    public ObtenerRestriccionesSalud(AlumnoRepository alumnoRepository) {
        this.alumnoRepository = alumnoRepository;
    }

    public Set<ClasificacionSalud> ejecutar(UUID alumnoId) {

        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("El alumno con ID " + alumnoId +
                        " no existe."));

        Set<ClasificacionSalud> restricciones = alumno.getRestriccionesSalud();

        return restricciones;
    }
}
