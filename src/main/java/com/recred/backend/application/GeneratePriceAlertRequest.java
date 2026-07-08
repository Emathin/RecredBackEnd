package com.recred.backend.application;

import com.recred.backend.domain.models.Producto;

import java.math.BigDecimal;
import java.util.UUID;

public record GeneratePriceAlertRequest(
        Producto product,
        BigDecimal oldPrice,
        BigDecimal newPrice,
        UUID buffetId
) {
}
