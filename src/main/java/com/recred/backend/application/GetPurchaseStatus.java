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

    public GetPurchaseStatusResponse execute(GetPurchaseStatusRequest request) {
        requireRequest(request);
        Purchase purchase = purchase(request.purchaseId());
        ConsumptionRequester requester = requester(request.cognitoSubject());
        requireAccess(requester, purchase, request.purchaseId());
        return GetPurchaseStatusResponse.from(purchase);
    }

    private void requireRequest(GetPurchaseStatusRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
    }

    private Purchase purchase(UUID purchaseId) {
        return purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new PurchaseNotFoundException(purchaseId));
    }

    private ConsumptionRequester requester(String cognitoSubject) {
        return accessRepository.findRequesterByCognitoSubject(cognitoSubject)
                .orElseThrow(() -> new UsuarioAutenticadoNoEncontradoException(cognitoSubject));
    }

    private void requireAccess(
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
        return requester.isStudentFor(studentId) || tutorUserCanAccessStudent(requester, studentId);
    }

    private boolean tutorUserCanAccessStudent(
            ConsumptionRequester requester,
            UUID studentId
    ) {
        return requester.isTutor()
                && accessRepository.tutorCanAccessStudent(requester.userId(), studentId);
    }
}
