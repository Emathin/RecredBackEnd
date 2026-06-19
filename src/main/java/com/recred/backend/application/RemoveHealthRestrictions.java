package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;

import java.util.UUID;

public class RemoveHealthRestrictions {
    private final AlumnoRepository alumnoRepository;

    public RemoveHealthRestrictions(AlumnoRepository alumnoRepository) {
        this.alumnoRepository = alumnoRepository;
    }

    public void execute(UUID studentId, UUID classificationId) {
        Alumno student = alumnoRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("El alumno no existe."));

        student.removeHealthRestriction(classificationId);

        alumnoRepository.save(student);
    }
}
