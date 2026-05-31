package com.recred.backend.application;

import com.recred.backend.domain.exceptions.InvalidPurchaseStatusTransitionException;
import com.recred.backend.domain.exceptions.PurchaseNotFoundException;
import com.recred.backend.domain.models.PurchaseState;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.PurchaseStatus;
import com.recred.backend.domain.repositories.PurchaseRepository;

import java.util.UUID;

public class UpdatePurchaseStatus {

    private final PurchaseRepository purchaseRepository;

    public UpdatePurchaseStatus(PurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    public PurchaseStatus execute(UUID purchaseId, PurchaseState requestedStatus) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new PurchaseNotFoundException(purchaseId));

        if (requestedStatus == PurchaseState.CANCELADO) {
            InvalidPurchaseStatusTransitionException exception =
                    new InvalidPurchaseStatusTransitionException(
                            purchase.getStatus(),
                            requestedStatus
                    );
            throw exception;
        }

        purchase.changeStatusTo(requestedStatus);
        Purchase savedPurchase = purchaseRepository.save(purchase);

        return PurchaseStatus.from(savedPurchase);
    }
}
