package com.recred.backend.domain.exceptions;

import com.recred.backend.domain.models.PurchaseState;

public class InvalidPurchaseStatusTransitionException extends RuntimeException {

    public InvalidPurchaseStatusTransitionException(
            PurchaseState currentStatus,
            PurchaseState requestedStatus
    ) {
        super("No se puede cambiar el stock de " + currentStatus + " a " + requestedStatus);
    }
}
