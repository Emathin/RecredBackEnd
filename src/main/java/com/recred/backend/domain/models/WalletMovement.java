package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalletMovement(
        UUID id,
        LocalDateTime dateTime,
        TipoMovimientoBilletera type,
        String description,
        BigDecimal amount,
        WalletMovementDirection direction
) {
}
