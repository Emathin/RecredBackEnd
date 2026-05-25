package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ClasificacionSaludRepository;

import java.util.UUID;

public class AddHealthRestriction {

    private final AlumnoRepository alumnoRepository;
    private final ClasificacionSaludRepository clasificacionRepository;

    public AddHealthRestriction(AlumnoRepository alumnoRepository,
                                ClasificacionSaludRepository clasificacionRepository) {
        this.alumnoRepository = alumnoRepository;
        this.clasificacionRepository = clasificacionRepository;
    }

    public void execute(UUID studentId, UUID classificationId) {
        Alumno student = alumnoRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("El alumno no existe."));

        ClasificacionSalud classification = clasificacionRepository.findById(classificationId)
                .orElseThrow(() -> new IllegalArgumentException
                        ("La clasificación no existe."));

        student.agregarRestriccionSalud(classification);

        alumnoRepository.save(student);
    }
}