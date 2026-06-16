package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.WalletMovement;
import com.recred.backend.domain.models.WalletMovementDirection;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalletMovementResponse(
        UUID id,
        LocalDateTime fechaHora,
        String tipo,
        String descripcion,
        BigDecimal monto,
        WalletMovementDirection direccion
) {
    public static WalletMovementResponse fromDomain(WalletMovement movement) {
        return new WalletMovementResponse(
                movement.id(),
                movement.dateTime(),
                movement.type().name(),
                movement.description(),
                movement.amount(),
                movement.direction()
        );
    }
}
