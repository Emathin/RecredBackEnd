package com.recred.backend.domain.exceptions;

import java.util.UUID;

public class PurchaseNotFoundException extends RuntimeException {

    public PurchaseNotFoundException(UUID purchaseId) {
        super("No existe la compra con id " + purchaseId);
    }
}
