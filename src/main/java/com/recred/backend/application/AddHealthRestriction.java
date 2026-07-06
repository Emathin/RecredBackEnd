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

    public AddHealthRestrictionResponse execute(AddHealthRestrictionRequest addRestrictionRequest) {
        Alumno student = findStudentById(addRestrictionRequest.studentId());
        ClasificacionSalud classification =
                findHealthClassificationById(addRestrictionRequest.classificationId());

        addHealthRestriction(student, classification);
        return new AddHealthRestrictionResponse();
    }

    private Alumno findStudentById(UUID studentId) {
        return alumnoRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("El alumno no existe."));
    }

    private ClasificacionSalud findHealthClassificationById(UUID classificationId) {
        return clasificacionRepository
                .findById(classificationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La clasificaciÃ³n mÃ©dica no existe."));
    }

    private void addHealthRestriction(Alumno student, ClasificacionSalud classification) {
        student.agregarRestriccionSalud(classification);
        alumnoRepository.save(student);
    }
}
