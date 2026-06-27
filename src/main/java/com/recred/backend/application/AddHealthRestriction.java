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

    public void execute(AddHealthRestrictionRequest request) {
        Alumno student = this.findAlumnoById(request.studentId());

        ClasificacionSalud classification =
                this.findClasificacionSaludById(request.classificationId());

        student.agregarRestriccionSalud(classification);

        alumnoRepository.save(student);
    }

    private Alumno findAlumnoById(UUID id) {
        return alumnoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El alumno no existe."));
    }

    private ClasificacionSalud findClasificacionSaludById(UUID id) {
        return clasificacionRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException
                        ("La clasificación médica no existe."));
    }
}