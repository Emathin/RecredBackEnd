package com.recred.backend.infrastructure.web;

import java.math.BigDecimal;
import java.util.UUID;

public record CarritoFavoritoItemResponse(
        UUID productId,
        String productName,
        BigDecimal unitPrice,
        Integer quantity
) {}
