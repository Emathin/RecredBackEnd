package com.recred.backend.application;

import com.recred.backend.domain.models.InventoryQuickActionType;
import java.util.UUID;

public record InventoryQuickActionCommand(
        UUID buffetId,
        UUID productId,
        InventoryQuickActionType action,
        Integer quantity,
        Integer stockMinimo,
        String motivo,
        UUID usuarioId
) {
}
