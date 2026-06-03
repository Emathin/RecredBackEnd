package com.recred.backend.application;

import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public class ObtenerHistorialTutor {

    private final PurchaseRepository purchaseRepository;

    public ObtenerHistorialTutor(PurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    @Transactional(readOnly = true)
    public List<Purchase> execute(UUID tutorId) {
        return purchaseRepository.findHistorialByTutor(tutorId);
    }
}