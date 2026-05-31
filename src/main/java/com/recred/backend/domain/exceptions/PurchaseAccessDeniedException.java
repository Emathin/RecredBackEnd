package com.recred.backend.domain.exceptions;

import java.util.UUID;

public class PurchaseAccessDeniedException extends RuntimeException {

    public PurchaseAccessDeniedException(UUID purchaseId) {
        super("No tiene permisos para acceder a la compra " + purchaseId);
    }
}
