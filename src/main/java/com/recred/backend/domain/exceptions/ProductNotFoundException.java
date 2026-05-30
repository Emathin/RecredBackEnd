package com.recred.backend.domain.exceptions;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(UUID productId) {
        super("Producto no encontrado con id: " + productId);
    }
}
