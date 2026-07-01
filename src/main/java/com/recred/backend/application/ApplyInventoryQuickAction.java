package com.recred.backend.application;

import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import org.springframework.transaction.annotation.Transactional;

public class ApplyInventoryQuickAction {

    private final InventoryAvailabilityService inventoryAvailabilityService;

    public ApplyInventoryQuickAction(InventoryAvailabilityService inventoryAvailabilityService) {
        this.inventoryAvailabilityService = inventoryAvailabilityService;
    }

    @Transactional
    public AdjustInventoryStockResponse execute(ApplyInventoryQuickActionRequest request) {
        Inventario inventory = inventoryAvailabilityService.applyQuickAction(
                request.buffetId(),
                request.productId(),
                request.action(),
                request.quantity(),
                request.stockMinimo(),
                request.motivo(),
                request.usuarioId()
        );
        return AdjustInventoryStockResponse.fromDomain(inventory);
    }
}
