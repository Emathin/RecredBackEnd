package com.recred.backend.application;

import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public class GetHistoryByStudent {

    private final PurchaseRepository purchaseRepository;

    public GetHistoryByStudent(PurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    @Transactional(readOnly = true)
    public List<GetHistoryByStudentResponse> execute(GetHistoryByStudentRequest historyRequest) {
        List<Purchase> purchases = findHistoryByStudent(historyRequest.alumnoId());

        return GetHistoryByStudentResponse.fromPurchases(purchases);
    }

    private List<Purchase> findHistoryByStudent(UUID studentId) {
        return purchaseRepository.findHistorialByAlumno(studentId);
    }
}
