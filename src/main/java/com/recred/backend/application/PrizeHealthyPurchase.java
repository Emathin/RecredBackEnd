package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.PurchaseNotFoundException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RewardLevel;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;

import java.time.LocalDateTime;

public class PrizeHealthyPurchase {

    private final PurchaseRepository purchaseRepository;
    private final AlumnoRepository alumnoRepository;

    public PrizeHealthyPurchase(PurchaseRepository purchaseRepository,
                                AlumnoRepository alumnoRepository) {
        this.purchaseRepository = purchaseRepository;
        this.alumnoRepository = alumnoRepository;
    }

    public void execute(PrizeHealthyPurchaseCommand command) {
        Purchase currentPurchase = purchaseRepository.findById(command.compraId())
                .orElseThrow(() -> new PurchaseNotFoundException(command.compraId()));

        if (currentPurchase.getSuggestionId() == null) {
            return;
        }

        long healthyPurchasesLast15Days = purchaseRepository.countHealthyPurchasesByAlumno(
                command.alumnoId(),
                LocalDateTime.now().minusDays(15)
        );

        long healthyPurchasesLast7Days = purchaseRepository.countHealthyPurchasesByAlumno(
                command.alumnoId(),
                LocalDateTime.now().minusDays(7)
        );

        RewardLevel level = determineLevel(healthyPurchasesLast7Days, healthyPurchasesLast15Days);

        Alumno alumno = alumnoRepository.findById(command.alumnoId())
                .orElseThrow(() -> new AlumnoNoEncontradoException(command.alumnoId()));

        alumno.addRewardPoints(level.getPoints());
        alumnoRepository.save(alumno);
    }

    private RewardLevel determineLevel(long last7Days, long last15Days) {
        if (last15Days > 5) {
            return RewardLevel.GOLD;
        }
        if (last7Days >= 3) {
            return RewardLevel.SILVER;
        }
        return RewardLevel.BRONZE;
    }
}
