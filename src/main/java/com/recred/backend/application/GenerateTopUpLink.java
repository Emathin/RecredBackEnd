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

    public GenerateTopUpLinkResponse execute(GenerateTopUpLinkRequest topUpRequest) {
        ensureStudentExists(topUpRequest);
        String paymentUrl = createTopUpPaymentUrl(topUpRequest);
        return new GenerateTopUpLinkResponse(paymentUrl);
    }

    private void ensureStudentExists(GenerateTopUpLinkRequest topUpRequest) {
        if (!alumnoRepository.existsById(topUpRequest.studentId())) {
            throw new RuntimeException("Alumno no encontrado");
        }
    }

    private String createTopUpPaymentUrl(GenerateTopUpLinkRequest topUpRequest) {
        return externalPaymentService.createTopUpPreference(
                topUpRequest.studentId(),
                topUpRequest.amount());
    }
}
