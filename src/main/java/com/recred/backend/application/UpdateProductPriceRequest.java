package com.recred.backend.application;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductPriceRequest(
        UUID productId,
        BigDecimal newPrice,
        UUID buffetId
) {
}
