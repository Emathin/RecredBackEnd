package com.recred.backend.application;

import com.recred.backend.domain.exceptions.PurchaseAccessDeniedException;
import com.recred.backend.domain.exceptions.PurchaseNotFoundException;
import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.ConsumptionRequester;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.PurchaseStatus;
import com.recred.backend.domain.models.RolUsuario;
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

    public PurchaseStatus execute(String cognitoSubject, UUID purchaseId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new PurchaseNotFoundException(purchaseId));
        ConsumptionRequester requester = accessRepository
                .findRequesterByCognitoSubject(cognitoSubject)
                .orElseThrow(() -> new UsuarioAutenticadoNoEncontradoException(cognitoSubject));

        if (requesterCannotAccessPurchase(requester, purchase)) {
            PurchaseAccessDeniedException exception = new PurchaseAccessDeniedException(purchaseId);
            throw exception;
        }

        return PurchaseStatus.from(purchase);
    }

    private boolean requesterCannotAccessPurchase(
            ConsumptionRequester requester,
            Purchase purchase
    ) {
        return purchase.getStudent() == null
                || purchase.getStudent().getId() == null
                || !requesterCanAccessStudent(requester, purchase.getStudent().getId());
    }

    private boolean requesterCanAccessStudent(
            ConsumptionRequester requester,
            UUID studentId
    ) {
        return studentUserCanAccessStudent(requester, studentId)
                || tutorUserCanAccessStudent(requester, studentId);
    }

    private boolean studentUserCanAccessStudent(
            ConsumptionRequester requester,
            UUID studentId
    ) {
        return requester.role() == RolUsuario.ALUMNO && studentId.equals(requester.studentId());
    }

    private boolean tutorUserCanAccessStudent(
            ConsumptionRequester requester,
            UUID studentId
    ) {
        return requester.role() == RolUsuario.PADRE
                && accessRepository.tutorCanAccessStudent(requester.userId(), studentId);
    }
}
