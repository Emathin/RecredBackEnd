package com.recred.backend.application;

import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import org.springframework.transaction.annotation.Transactional;

public class AdjustInventoryStock {

    private final InventoryAvailabilityService inventoryAvailabilityService;

    public AdjustInventoryStock(InventoryAvailabilityService inventoryAvailabilityService) {
        this.inventoryAvailabilityService = inventoryAvailabilityService;
    }

    @Transactional
    public AdjustInventoryStockResponse execute(AdjustInventoryStockRequest stockAdjustment) {
        Inventario inventory = adjustInventoryFrom(stockAdjustment);
        return AdjustInventoryStockResponse.fromDomain(inventory);
    }

    private Inventario adjustInventoryFrom(AdjustInventoryStockRequest stockAdjustment) {
        return inventoryAvailabilityService.adjustInventory(
                stockAdjustment.buffetId(),
                stockAdjustment.productId(),
                stockAdjustment.stockActual(),
                stockAdjustment.stockMinimo(),
                stockAdjustment.estadoInventario(),
                stockAdjustment.tipoManejoInventario(),
                stockAdjustment.disponible(),
                stockAdjustment.cupoMaximoDiario(),
                stockAdjustment.motivo(),
                stockAdjustment.usuarioId()
        );
    }
}
