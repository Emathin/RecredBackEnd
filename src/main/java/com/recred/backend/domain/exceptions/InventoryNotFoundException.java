package com.recred.backend.domain.exceptions;

import java.util.UUID;

public class InventoryNotFoundException extends RuntimeException {
    public InventoryNotFoundException(UUID buffetId, UUID productId) {
        super("No se encontro inventario para buffet " + buffetId
                + " y producto " + productId);
    }
}
