package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.PurchaseNotFoundException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RewardLevel;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public class PrizeHealthyPurchase {

    private final PurchaseRepository purchaseRepository;
    private final AlumnoRepository alumnoRepository;

    public PrizeHealthyPurchase(
            PurchaseRepository purchaseRepository,
            AlumnoRepository alumnoRepository
    ) {
        this.purchaseRepository = purchaseRepository;
        this.alumnoRepository = alumnoRepository;
    }

    public PrizeHealthyPurchaseResponse execute(PrizeHealthyPurchaseRequest prizeRequest) {
        Purchase purchase = findPurchase(prizeRequest.compraId());
        prizeStudentIfPurchaseHasSuggestion(prizeRequest.alumnoId(), purchase);

        return new PrizeHealthyPurchaseResponse();
    }

    private void prizeStudentIfPurchaseHasSuggestion(UUID studentId, Purchase purchase) {
        if (!purchase.hasSuggestion()) {
            return;
        }

        prizeStudentForHealthyPurchase(studentId);
    }

    private void prizeStudentForHealthyPurchase(UUID studentId) {
        Alumno student = findStudent(studentId);
        student.addRewardPoints(resolveRewardLevel(studentId).getPoints());
        alumnoRepository.save(student);
    }

    private Purchase findPurchase(UUID purchaseId) {
        return purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new PurchaseNotFoundException(purchaseId));
    }

    private Alumno findStudent(UUID studentId) {
        return alumnoRepository.findById(studentId)
                .orElseThrow(() -> new AlumnoNoEncontradoException(studentId));
    }

    private RewardLevel resolveRewardLevel(UUID studentId) {
        long last15Days = healthyPurchasesSince(studentId, 15);
        long last7Days = healthyPurchasesSince(studentId, 7);
        return RewardLevel.forHealthyPurchaseHistory(last7Days, last15Days);
    }

    private long healthyPurchasesSince(UUID studentId, int days) {
        return purchaseRepository.countHealthyPurchasesByAlumno(
                studentId,
                LocalDateTime.now().minusDays(days)
        );
    }
}
