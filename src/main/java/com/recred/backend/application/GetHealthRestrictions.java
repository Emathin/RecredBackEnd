package com.recred.backend.application;

import com.recred.backend.domain.models.HealthClassification;
import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.repositories.StudentRepository;

import java.util.Set;
import java.util.UUID;

public class GetHealthRestrictions {

    private final StudentRepository studentRepository;

    public GetHealthRestrictions(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public Set<HealthClassification> ejecutar(UUID alumnoId) {

        Student student = studentRepository.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("El alumno con ID " + alumnoId +
                        " no existe."));

        Set<HealthClassification> restricciones = student.getHealthRestrictions();

        return restricciones;
    }
}
