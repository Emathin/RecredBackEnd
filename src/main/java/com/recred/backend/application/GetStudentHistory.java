package com.recred.backend.application;

import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public class GetStudentHistory {

    private final PurchaseRepository purchaseRepository;

    public GetStudentHistory(PurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    @Transactional(readOnly = true)
    public List<Purchase> execute(UUID studentId) {
        return purchaseRepository.findHistorialByAlumno(studentId);
    }
}
