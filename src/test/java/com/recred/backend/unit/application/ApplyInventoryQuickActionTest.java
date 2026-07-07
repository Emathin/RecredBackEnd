package com.recred.backend.unit.application;

import com.recred.backend.application.AdjustInventoryStockResponse;
import com.recred.backend.application.ApplyInventoryQuickAction;
import com.recred.backend.application.ApplyInventoryQuickActionRequest;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.models.InventoryQuickActionType;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplyInventoryQuickActionTest {

    private InventoryAvailabilityService inventoryAvailabilityService;
    private ApplyInventoryQuickAction applyInventoryQuickAction;
    private UUID buffetId;
    private UUID productId;
    private UUID userId;
    private Inventario inventory;
    private ApplyInventoryQuickActionRequest quickActionRequest;
    private AdjustInventoryStockResponse response;

    @BeforeEach
    void setUp() {
        inventoryAvailabilityService = mock(InventoryAvailabilityService.class);
        applyInventoryQuickAction = new ApplyInventoryQuickAction(inventoryAvailabilityService);
    }

    @Test
    void givenQuickActionRequest_whenApplyingQuickAction_thenDelegatesAndReturnsResponse() {
        givenQuickActionRequest();
        givenQuickActionWillBeApplied();

        whenApplyingQuickAction();

        thenQuickActionWasDelegated();
        thenInventoryResponseMatchesSavedInventory();
    }

    private void givenQuickActionRequest() {
        buffetId = randomUUID();
        productId = randomUUID();
        userId = randomUUID();
        inventory = inventory(buffetId, productId);
        quickActionRequest = quickActionRequest();
    }

    private void givenQuickActionWillBeApplied() {
        when(inventoryAvailabilityService.applyQuickAction(
                buffetId,
                productId,
                InventoryQuickActionType.ADD_STOCK,
                5,
                2,
                "Ingreso",
                userId
        )).thenReturn(inventory);
    }

    private void whenApplyingQuickAction() {
        response = applyInventoryQuickAction.execute(quickActionRequest);
    }

    private void thenQuickActionWasDelegated() {
        verify(inventoryAvailabilityService).applyQuickAction(
                buffetId,
                productId,
                InventoryQuickActionType.ADD_STOCK,
                5,
                2,
                "Ingreso",
                userId
        );
    }

    private void thenInventoryResponseMatchesSavedInventory() {
        assertEquals(inventory.getId(), response.id());
        assertEquals(buffetId, response.buffetId());
        assertEquals(productId, response.productoId());
        assertEquals(12, response.stockActual());
        assertEquals(10, response.stockDisponible());
        assertEquals("DISPONIBLE", response.estadoInventario());
        assertEquals("STOCK_EXACTO", response.tipoManejoInventario());
    }

    private ApplyInventoryQuickActionRequest quickActionRequest() {
        return new ApplyInventoryQuickActionRequest(
                buffetId,
                productId,
                InventoryQuickActionType.ADD_STOCK,
                5,
                2,
                "Ingreso",
                userId
        );
    }

    private Inventario inventory(UUID buffetId, UUID productId) {
        Inventario inventory = new Inventario();
        inventory.setId(UUID.randomUUID());
        inventory.setBuffetId(buffetId);
        inventory.setProductoId(productId);
        inventory.setStockActual(12);
        inventory.setStockReservado(2);
        inventory.setStockMinimo(1);
        inventory.setEstado(EstadoInventario.DISPONIBLE);
        inventory.setTipoManejo(TipoManejoInventario.STOCK_EXACTO);
        return inventory;
    }
}
