package com.recred.backend.application;

import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.models.HealthClassification;
import com.recred.backend.domain.repositories.StudentRepository;
import com.recred.backend.domain.repositories.HealthClassificationRepository;

import java.util.UUID;

public class AddHealthRestriction {

    private final StudentRepository studentRepository;
    private final HealthClassificationRepository clasificacionRepository;

    public AddHealthRestriction(StudentRepository studentRepository,
                                HealthClassificationRepository clasificacionRepository) {
        this.studentRepository = studentRepository;
        this.clasificacionRepository = clasificacionRepository;
    }

    public void execute(UUID studentId, UUID classificationId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("El alumno no existe."));

        HealthClassification classification = clasificacionRepository.findById(classificationId)
                .orElseThrow(() -> new IllegalArgumentException
                        ("La clasificación médica no existe."));

        student.addHealthRestriction(classification);

        studentRepository.save(student);
    }
}