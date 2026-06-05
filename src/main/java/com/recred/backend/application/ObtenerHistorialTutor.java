package com.recred.backend.application;

import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public class ObtenerHistorialTutor {

    private final PurchaseRepository purchaseRepository;
    private final TutorRepository tutorRepository;

    public ObtenerHistorialTutor(PurchaseRepository purchaseRepository,
                                  TutorRepository tutorRepository) {
        this.purchaseRepository = purchaseRepository;
        this.tutorRepository = tutorRepository;
    }

    @Transactional(readOnly = true)
    public List<Purchase> execute(UUID tutorId) {
        return purchaseRepository.findHistorialByTutor(tutorId);
    }

    @Transactional(readOnly = true)
    public List<Purchase> executeByCognitoSubject(String cognitoSubject) {
        UUID tutorId = tutorRepository.findByUsuarioIdCognito(cognitoSubject)
                .orElseThrow(() -> new RuntimeException(
                        "Tutor no encontrado para el usuario autenticado"))
                .getId();
        return purchaseRepository.findHistorialByTutor(tutorId);
    }
}