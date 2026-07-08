package com.recred.backend.unit.application;

import com.recred.backend.application.AdjustInventoryStock;
import com.recred.backend.application.AdjustInventoryStockRequest;
import com.recred.backend.application.AdjustInventoryStockResponse;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdjustInventoryStockTest {

    private InventoryAvailabilityService inventoryAvailabilityService;
    private AdjustInventoryStock useCase;
    private UUID buffetId;
    private UUID productId;
    private UUID userId;
    private Inventario inventory;
    private AdjustInventoryStockResponse response;

    @BeforeEach
    void setUp() {
        inventoryAvailabilityService = mock(InventoryAvailabilityService.class);
        useCase = new AdjustInventoryStock(inventoryAvailabilityService);
        buffetId = UUID.randomUUID();
        productId = UUID.randomUUID();
        userId = UUID.randomUUID();
        inventory = inventory(buffetId, productId);
    }

    @Test
    void inventoryAdjustmentDelegatesToAvailabilityServiceAndReturnsUpdatedStock() {
        givenInventoryCanBeAdjusted();

        whenAdjustingInventory();

        thenInventoryAdjustmentWasRequested();
        thenResponseMatchesAdjustedInventory();
    }

    private void givenInventoryCanBeAdjusted() {
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
    }

    private void whenAdjustingInventory() {
        response = useCase.execute(inventoryAdjustmentRequest());
    }

    private void thenInventoryAdjustmentWasRequested() {
        verify(inventoryAvailabilityService).adjustInventory(
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
    }

    private void thenResponseMatchesAdjustedInventory() {
        assertEquals(inventory.getId(), response.id());
        assertEquals(buffetId, response.buffetId());
        assertEquals(productId, response.productoId());
        assertEquals(12, response.stockActual());
        assertEquals(10, response.stockDisponible());
        assertEquals("DISPONIBLE", response.estadoInventario());
        assertEquals("STOCK_EXACTO", response.tipoManejoInventario());
    }

    private AdjustInventoryStockRequest inventoryAdjustmentRequest() {
        return new AdjustInventoryStockRequest(
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
