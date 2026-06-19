package com.recred.backend.unit.application;

import com.recred.backend.application.*;

import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.models.InventoryQuickActionType;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdjustInventoryStockTest {

    private InventoryAvailabilityService inventoryAvailabilityService;
    private AdjustInventoryStock adjustInventoryStock;

    @BeforeEach
    void setUp() {
        inventoryAvailabilityService = mock(InventoryAvailabilityService.class);
        adjustInventoryStock = new AdjustInventoryStock(inventoryAvailabilityService);
    }

    @Test
    void executeWithSimpleParametersShouldDelegateToAdjustStock() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Inventario inventory = inventory(buffetId, productId);
        when(inventoryAvailabilityService.adjustStock(
                buffetId,
                productId,
                10,
                "Ajuste",
                userId
        )).thenReturn(inventory);

        Inventario result = adjustInventoryStock.execute(
                buffetId,
                productId,
                10,
                "Ajuste",
                userId
        );

        assertEquals(inventory, result);
        verify(inventoryAvailabilityService).adjustStock(
                buffetId,
                productId,
                10,
                "Ajuste",
                userId
        );
    }

    @Test
    void executeWithCommandShouldDelegateToAdjustInventory() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AdjustInventoryStockCommand command = new AdjustInventoryStockCommand(
                buffetId,
                productId,
                10,
                3,
                EstadoInventario.DISPONIBLE,
                TipoManejoInventario.STOCK_EXACTO,
                true,
                null,
                "Ajuste",
                userId
        );
        Inventario inventory = inventory(buffetId, productId);
        when(inventoryAvailabilityService.adjustInventory(
                buffetId,
                productId,
                10,
                3,
                EstadoInventario.DISPONIBLE,
                TipoManejoInventario.STOCK_EXACTO,
                true,
                null,
                "Ajuste",
                userId
        )).thenReturn(inventory);

        Inventario result = adjustInventoryStock.execute(command);

        assertEquals(inventory, result);
    }

    @Test
    void executeWithQuickActionShouldDelegateToApplyQuickAction() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        InventoryQuickActionCommand command = new InventoryQuickActionCommand(
                buffetId,
                productId,
                InventoryQuickActionType.ADD_STOCK,
                5,
                2,
                "Ingreso",
                userId
        );
        Inventario inventory = inventory(buffetId, productId);
        when(inventoryAvailabilityService.applyQuickAction(
                buffetId,
                productId,
                InventoryQuickActionType.ADD_STOCK,
                5,
                2,
                "Ingreso",
                userId
        )).thenReturn(inventory);

        Inventario result = adjustInventoryStock.execute(command);

        assertEquals(inventory, result);
    }

    private Inventario inventory(UUID buffetId, UUID productId) {
        Inventario inventory = new Inventario();
        inventory.setId(UUID.randomUUID());
        inventory.setBuffetId(buffetId);
        inventory.setProductoId(productId);
        return inventory;
    }
}
