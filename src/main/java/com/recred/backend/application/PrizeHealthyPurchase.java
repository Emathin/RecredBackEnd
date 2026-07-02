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

    public PrizeHealthyPurchase(PurchaseRepository purchaseRepository,
                                AlumnoRepository alumnoRepository) {
        this.purchaseRepository = purchaseRepository;
        this.alumnoRepository = alumnoRepository;
    }

    public PrizeHealthyPurchaseResponse execute(PrizeHealthyPurchaseRequest request) {
        Purchase currentPurchase = findPurchase(request.compraId());
        if (!currentPurchase.hasSuggestion()) {
            return new PrizeHealthyPurchaseResponse();
        }

        reward(request.alumnoId());
        return new PrizeHealthyPurchaseResponse();
    }

    private void reward(UUID alumnoId) {
        Alumno alumno = findAlumno(alumnoId);
        alumno.addRewardPoints(rewardLevel(alumnoId).getPoints());
        alumnoRepository.save(alumno);
    }

    private Purchase findPurchase(UUID purchaseId) {
        return purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new PurchaseNotFoundException(purchaseId));
    }

    private Alumno findAlumno(UUID alumnoId) {
        return alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new AlumnoNoEncontradoException(alumnoId));
    }

    private RewardLevel rewardLevel(UUID alumnoId) {
        long last15Days = healthyPurchasesSince(alumnoId, 15);
        long last7Days = healthyPurchasesSince(alumnoId, 7);
        return RewardLevel.forHealthyPurchaseHistory(last7Days, last15Days);
    }

    private long healthyPurchasesSince(UUID alumnoId, int days) {
        return purchaseRepository.countHealthyPurchasesByAlumno(
                alumnoId,
                LocalDateTime.now().minusDays(days)
        );
    }
}
