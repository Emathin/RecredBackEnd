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

    public GenerateTopUpLinkResponse execute(GenerateTopUpLinkRequest request) {

        if (!alumnoRepository.existsById(request.studentId())) {
            throw new RuntimeException("Alumno no encontrado");
        }


        return new GenerateTopUpLinkResponse(externalPaymentService.createTopUpPreference(
                request.studentId(), request.amount()));
    }
}
