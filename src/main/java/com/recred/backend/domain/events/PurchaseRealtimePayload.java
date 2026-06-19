package com.recred.backend.domain.events;

import com.recred.backend.domain.models.EstadoCompra;
import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseRealtimePayload(
        UUID purchaseId,
        EstadoCompra purchaseStatus,
        BigDecimal purchaseTotal
) implements KiosqueroRealtimePayload {
}
