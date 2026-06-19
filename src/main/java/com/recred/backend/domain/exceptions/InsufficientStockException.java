package com.recred.backend.domain.exceptions;

import java.util.UUID;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(UUID productId, int requested, int available) {
        super("Stock insuficiente para producto " + productId
                + ". Solicitado: " + requested
                + ". Disponible: " + available);
    }
}
