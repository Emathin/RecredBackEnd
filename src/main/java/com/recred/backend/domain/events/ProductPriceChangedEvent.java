package com.recred.backend.domain.events;

import com.recred.backend.domain.models.Producto;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductPriceChangedEvent(
        Producto producto,
        BigDecimal oldPrice,
        BigDecimal newPrice,
        UUID buffetId
) {
}
