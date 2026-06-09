package com.recred.backend.application;

import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.services.ExternalPaymentService;

public class GenerateTopUpLink {

    private final AlumnoRepository alumnoRepository;
    private final ExternalPaymentService externalPaymentService;

    public GenerateTopUpLink(
            AlumnoRepository alumnoRepository,
            ExternalPaymentService externalPaymentService) {
        this.alumnoRepository = alumnoRepository;
        this.externalPaymentService = externalPaymentService;
    }

    public String execute(TopUpCommand command) {

        if (!alumnoRepository.existsById(command.studentId())) {
            throw new RuntimeException("Alumno no encontrado");
        }


        return externalPaymentService.createTopUpPreference(
                command.studentId(), command.amount());
    }
}
