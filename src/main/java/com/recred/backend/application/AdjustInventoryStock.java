package com.recred.backend.application;

import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class AdjustInventoryStock {

    private final InventoryAvailabilityService inventoryAvailabilityService;

    public AdjustInventoryStock(InventoryAvailabilityService inventoryAvailabilityService) {
        this.inventoryAvailabilityService = inventoryAvailabilityService;
    }

    @Transactional
    public Inventario execute(
            UUID buffetId,
            UUID productId,
            int stockActual,
            String motivo,
            UUID usuarioId
    ) {
        return inventoryAvailabilityService.adjustStock(
                buffetId,
                productId,
                stockActual,
                motivo,
                usuarioId
        );
    }

    @Transactional
    public Inventario execute(AdjustInventoryStockCommand command) {
        return inventoryAvailabilityService.adjustInventory(
                command.buffetId(),
                command.productId(),
                command.stockActual(),
                command.stockMinimo(),
                command.estadoInventario(),
                command.tipoManejoInventario(),
                command.disponible(),
                command.cupoMaximoDiario(),
                command.motivo(),
                command.usuarioId()
        );
    }

    @Transactional
    public Inventario execute(InventoryQuickActionCommand command) {
        return inventoryAvailabilityService.applyQuickAction(
                command.buffetId(),
                command.productId(),
                command.action(),
                command.quantity(),
                command.stockMinimo(),
                command.motivo(),
                command.usuarioId()
        );
    }
}
