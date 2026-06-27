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

    public void execute(ManageRestrictionsRequest request) {
        Alumno student = this.findStudentById(request.studentId());
        Set<ClasificacionSalud> newRestrictions =
                getRestrictionsByIds(request.newClassificationIds());
        student.setRestriccionesSalud(newRestrictions);
        studentRepository.save(student);
    }

    private Alumno findStudentById(UUID studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El alumno con ID " + studentId + " no existe."
                ));
    }

    private Set<ClasificacionSalud> getRestrictionsByIds(List<UUID> classificationIds) {
        return new HashSet<>(healthClassificationRepository.findAllById(classificationIds));
    }
}