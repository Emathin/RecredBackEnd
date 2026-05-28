package com.recred.backend.domain.exceptions;

import com.recred.backend.domain.models.EstadoCompra;

public class InvalidPurchaseStatusTransitionException extends RuntimeException {

    public InvalidPurchaseStatusTransitionException(
            EstadoCompra currentStatus,
            EstadoCompra requestedStatus
    ) {
        super("No se puede cambiar el estado de " + currentStatus + " a " + requestedStatus);
    }
}
