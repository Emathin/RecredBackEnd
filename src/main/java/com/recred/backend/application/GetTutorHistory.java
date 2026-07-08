package com.recred.backend.application;

import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import java.util.List;
import java.util.UUID;

public class GetTutorHistory {

    private final PurchaseRepository purchaseRepository;
    private final TutorRepository tutorRepository;

    public GetTutorHistory(PurchaseRepository purchaseRepository,
                                  TutorRepository tutorRepository) {
        this.purchaseRepository = purchaseRepository;
        this.tutorRepository = tutorRepository;
    }

    public GetTutorHistoryResponse execute(GetTutorHistoryRequest request) {
        List<Purchase> purchases = purchaseRepository.findHistorialByTutor(tutorId(request));

        return new GetTutorHistoryResponse(GetHistoryByStudentResponse.fromPurchases(purchases));
    }

    private UUID tutorId(GetTutorHistoryRequest request) {
        if (request.tutorId() != null) {
            return request.tutorId();
        }

        return tutorRepository.findByUsuarioIdCognito(request.cognitoSubject())
                .orElseThrow(() -> new RuntimeException(
                        "Tutor no encontrado para el usuario autenticado"))
                .getId();
    }
}
