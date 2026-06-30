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
    public AdjustInventoryStockResponse execute(AdjustInventoryStockRequest request) {
        Inventario inventory = adjustInventory(request);
        return AdjustInventoryStockResponse.fromDomain(inventory);
    }
    private Inventario adjustInventory(AdjustInventoryStockRequest request) {
        return inventoryAvailabilityService.adjustInventory(
                request.buffetId(),
                request.productId(),
                request.stockActual(),
                request.stockMinimo(),
                request.estadoInventario(),
                request.tipoManejoInventario(),
                request.disponible(),
                request.cupoMaximoDiario(),
                request.motivo(),
                request.usuarioId()
        );
    }
}
