package com.recred.backend.application;

import com.recred.backend.domain.exceptions.PurchaseAccessDeniedException;
import com.recred.backend.domain.exceptions.PurchaseNotFoundException;
import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.ConsumptionRequester;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.ConsumptionAccessRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;

import java.util.UUID;

public class GetPurchaseStatus {

    private final PurchaseRepository purchaseRepository;
    private final ConsumptionAccessRepository accessRepository;

    public GetPurchaseStatus(
            PurchaseRepository purchaseRepository,
            ConsumptionAccessRepository accessRepository
    ) {
        this.purchaseRepository = purchaseRepository;
        this.accessRepository = accessRepository;
    }

    public GetPurchaseStatusResponse execute(GetPurchaseStatusRequest statusRequest) {
        Purchase purchase = findPurchase(statusRequest.purchaseId());
        ConsumptionRequester requester = findAuthenticatedRequester(
                statusRequest.cognitoSubject());
        ensureRequesterCanReadPurchaseStatus(requester, purchase, statusRequest.purchaseId());
        return GetPurchaseStatusResponse.from(purchase);
    }

    private Purchase findPurchase(UUID purchaseId) {
        return purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new PurchaseNotFoundException(purchaseId));
    }

    private ConsumptionRequester findAuthenticatedRequester(String cognitoSubject) {
        return accessRepository.findRequesterByCognitoSubject(cognitoSubject)
                .orElseThrow(() -> new UsuarioAutenticadoNoEncontradoException(cognitoSubject));
    }

    private void ensureRequesterCanReadPurchaseStatus(
            ConsumptionRequester requester,
            Purchase purchase,
            UUID purchaseId
    ) {
        if (!purchase.hasStudentId()
                || !requesterCanAccessStudent(requester, purchase.studentIdOrNull())) {
            throw new PurchaseAccessDeniedException(purchaseId);
        }
    }

    private boolean requesterCanAccessStudent(ConsumptionRequester requester, UUID studentId) {
        return requester.isStudentFor(studentId) || tutorCanAccessStudent(requester, studentId);
    }

    private boolean tutorCanAccessStudent(
            ConsumptionRequester requester,
            UUID studentId
    ) {
        return requester.isTutor()
                && accessRepository.tutorCanAccessStudent(requester.userId(), studentId);
    }
}
