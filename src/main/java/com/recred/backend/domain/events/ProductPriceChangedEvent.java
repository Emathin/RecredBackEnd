package com.recred.backend.domain.events;

import com.recred.backend.domain.models.Product;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductPriceChangedEvent(
        Product product,
        BigDecimal oldPrice,
        BigDecimal newPrice,
        UUID buffetId
) {
}
