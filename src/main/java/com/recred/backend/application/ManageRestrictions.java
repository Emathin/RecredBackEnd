package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ClasificacionSaludRepository;
import java.util.HashSet;
import java.util.Set;

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

    public void execute(ManageRestrictionsRequest request) {
        Alumno student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "El alumno con ID " + request.studentId() + " no existe."
                ));

        Set<ClasificacionSalud> newRestrictions = new HashSet<>(
                healthClassificationRepository.findAllById(request.newClassificationIds())
        );

        student.setRestriccionesSalud(newRestrictions);

        studentRepository.save(student);
    }
}