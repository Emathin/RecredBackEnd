package com.recred.backend.application;

import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.models.HealthClassification;
import com.recred.backend.domain.repositories.StudentRepository;
import com.recred.backend.domain.repositories.HealthClassificationRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ManageRestrictions {

    private final StudentRepository studentRepository;
    private final HealthClassificationRepository healthClassificationRepository;

    public ManageRestrictions(
            StudentRepository studentRepository,
            HealthClassificationRepository healthClassificationRepository
    ) {
        this.studentRepository = studentRepository;
        this.healthClassificationRepository = healthClassificationRepository;
    }

    public void execute(UUID studentId, List<UUID> newClassificationIds) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El alumno con ID " + studentId + " no existe."
                ));

        Set<HealthClassification> newRestrictions = new HashSet<>(
                healthClassificationRepository.findAllById(newClassificationIds)
        );

        student.setHealthRestrictions(newRestrictions);

        studentRepository.save(student);
    }
}