package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ClasificacionSaludRepository;

import java.util.UUID;

public class AgregarRestriccionSalud {

    private final AlumnoRepository alumnoRepository;
    private final ClasificacionSaludRepository clasificacionRepository;

    public AgregarRestriccionSalud(AlumnoRepository alumnoRepository,
                                   ClasificacionSaludRepository clasificacionRepository) {
        this.alumnoRepository = alumnoRepository;
        this.clasificacionRepository = clasificacionRepository;
    }

    public void ejecutar(UUID alumnoId, UUID clasificacionId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("El alumno no existe."));

        ClasificacionSalud clasificacion = clasificacionRepository.findById(clasificacionId)
                .orElseThrow(() -> new IllegalArgumentException
                        ("La clasificación médica no existe."));

        alumno.agregarRestriccionSalud(clasificacion);

        alumnoRepository.save(alumno);
    }
}