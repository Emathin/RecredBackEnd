package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetStudentWalletSummaryResponse;
import com.recred.backend.domain.models.WalletMovement;
import com.recred.backend.domain.models.WalletMovementDirection;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalletMovementResponseDTO(
        UUID id,
        LocalDateTime fechaHora,
        String tipo,
        String descripcion,
        BigDecimal monto,
        WalletMovementDirection direccion
) {
    public static WalletMovementResponseDTO fromDomain(WalletMovement movement) {
        return new WalletMovementResponseDTO(
                movement.id(),
                movement.dateTime(),
                movement.type().name(),
                movement.description(),
                movement.amount(),
                movement.direction()
        );
    }

    public static WalletMovementResponseDTO fromApplication(
            GetStudentWalletSummaryResponse.Movement movement) {
        return new WalletMovementResponseDTO(
                movement.id(),
                movement.dateTime(),
                movement.type(),
                movement.description(),
                movement.amount(),
                movement.direction()
        );
    }
}
