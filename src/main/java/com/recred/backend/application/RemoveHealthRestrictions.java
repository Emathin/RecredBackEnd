package com.recred.backend.application;

import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.repositories.StudentRepository;

import java.util.UUID;

public class RemoveHealthRestrictions {
    private final StudentRepository studentRepository;

    public RemoveHealthRestrictions(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public void ejecutar(UUID alumnoId, UUID clasificacionId) {
        Student student = studentRepository.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("El alumno no existe."));

        student.removeHealthRestriction(clasificacionId);

        studentRepository.save(student);
    }
}
