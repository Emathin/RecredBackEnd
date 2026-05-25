package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ClasificacionSaludRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ManageRestrictions {

    private final AlumnoRepository studentRepository;
    private final ClasificacionSaludRepository healthClassificationRepository;

    public ManageRestrictions(
            AlumnoRepository studentRepository,
            ClasificacionSaludRepository healthClassificationRepository
    ) {
        this.studentRepository = studentRepository;
        this.healthClassificationRepository = healthClassificationRepository;
    }

    public void execute(UUID studentId, List<UUID> newClassificationIds) {
        Alumno student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "The student with ID " + studentId + " does not exist."
                ));

        Set<ClasificacionSalud> newRestrictions = new HashSet<>(
                healthClassificationRepository.findAllById(newClassificationIds)
        );

        student.setRestriccionesSalud(newRestrictions);

        studentRepository.save(student);
    }
}