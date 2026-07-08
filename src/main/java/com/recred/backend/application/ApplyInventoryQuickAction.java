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
    public AdjustInventoryStockResponse execute(
            ApplyInventoryQuickActionRequest inventoryActionRequest) {
        Inventario updatedInventory = updateInventoryFromQuickAction(inventoryActionRequest);
        return AdjustInventoryStockResponse.fromDomain(updatedInventory);
    }

    private Inventario updateInventoryFromQuickAction(
            ApplyInventoryQuickActionRequest inventoryActionRequest) {
        return inventoryAvailabilityService.applyQuickAction(
                inventoryActionRequest.buffetId(),
                inventoryActionRequest.productId(),
                inventoryActionRequest.action(),
                inventoryActionRequest.quantity(),
                inventoryActionRequest.stockMinimo(),
                inventoryActionRequest.motivo(),
                inventoryActionRequest.usuarioId()
        );
    }
}
