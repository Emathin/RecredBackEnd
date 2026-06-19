package com.recred.backend.unit.domain.services;

import com.recred.backend.domain.services.*;

import com.recred.backend.domain.exceptions.InventoryOperationException;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.models.InventoryQuickActionType;
import com.recred.backend.domain.models.TipoManejoInventario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryQuickActionProcessorTest {

    private final InventoryQuickActionProcessor processor = new InventoryQuickActionProcessor();

    @Test
    void apply_AddStock_ShouldIncreaseCurrentStock() {
        Inventario inventory = inventory(4, 1);

        processor.apply(inventory, InventoryQuickActionType.ADD_STOCK, 3, 2);

        assertEquals(TipoManejoInventario.STOCK_EXACTO, inventory.getTipoManejo());
        assertEquals(7, inventory.getStockActual());
        assertEquals(2, inventory.getStockMinimo());
    }

    @Test
    void apply_SubtractStock_ShouldNotGoBelowReservedStock() {
        Inventario inventory = inventory(4, 2);

        processor.apply(inventory, InventoryQuickActionType.SUBTRACT_STOCK, 10, null);

        assertEquals(2, inventory.getStockActual());
    }

    @Test
    void apply_MarkSoldOutInAvailabilityMode_ShouldOnlyChangeStatus() {
        Inventario inventory = inventory(8, 2);
        inventory.setTipoManejo(TipoManejoInventario.DISPONIBLE_NO_DISPONIBLE);

        processor.apply(inventory, InventoryQuickActionType.MARK_SOLD_OUT, null, null);

        assertEquals(8, inventory.getStockActual());
        assertEquals(EstadoInventario.SIN_STOCK, inventory.getEstado());
    }

    @Test
    void apply_SetAvailable_ShouldUseAvailabilityMode() {
        Inventario inventory = inventory(0, 0);
        inventory.setEstado(EstadoInventario.SIN_STOCK);

        processor.apply(inventory, InventoryQuickActionType.SET_AVAILABLE, null, null);

        assertEquals(TipoManejoInventario.DISPONIBLE_NO_DISPONIBLE,
                inventory.getTipoManejo());
        assertEquals(EstadoInventario.DISPONIBLE, inventory.getEstado());
    }

    @Test
    void apply_WhenActionIsMissing_ShouldFail() {
        assertThrows(InventoryOperationException.class,
                () -> processor.apply(inventory(1, 0), null, null, null));
    }

    @Test
    void apply_WhenRequiredQuantityIsMissingOrInvalid_ShouldFail() {
        assertThrows(InventoryOperationException.class,
                () -> processor.apply(
                        inventory(1, 0),
                        InventoryQuickActionType.SET_STOCK,
                        null,
                        null
                ));
        assertThrows(InventoryOperationException.class,
                () -> processor.apply(
                        inventory(1, 0),
                        InventoryQuickActionType.SET_STOCK,
                        -1,
                        null
                ));
        assertThrows(InventoryOperationException.class,
                () -> processor.apply(
                        inventory(1, 0),
                        InventoryQuickActionType.ADD_STOCK,
                        0,
                        null
                ));
    }

    @Test
    void apply_WhenMinimumStockIsNegative_ShouldFail() {
        assertThrows(InventoryOperationException.class,
                () -> processor.apply(
                        inventory(1, 0),
                        InventoryQuickActionType.SET_STOCK,
                        1,
                        -1
                ));
    }

    @Test
    void reason_ShouldUseCustomReasonOrDefaultReason() {
        assertEquals("Ajuste manual",
                processor.reason(InventoryQuickActionType.SET_STOCK, "Ajuste manual"));
        assertEquals("Reposicion de stock",
                processor.reason(InventoryQuickActionType.ADD_STOCK, " "));
        assertEquals("Descuento manual de stock",
                processor.reason(InventoryQuickActionType.SUBTRACT_STOCK, null));
        assertEquals("Producto marcado como agotado",
                processor.reason(InventoryQuickActionType.MARK_SOLD_OUT, null));
        assertEquals("Producto marcado como disponible",
                processor.reason(InventoryQuickActionType.SET_AVAILABLE, null));
        assertEquals("Producto pausado",
                processor.reason(InventoryQuickActionType.SET_UNAVAILABLE, null));
        assertEquals("Cupo diario definido",
                processor.reason(InventoryQuickActionType.SET_DAILY_CAPACITY, null));
    }

    private Inventario inventory(int stockActual, int stockReservado) {
        Inventario inventory = new Inventario();
        inventory.setStockActual(stockActual);
        inventory.setStockReservado(stockReservado);
        inventory.setStockMinimo(1);
        inventory.setEstado(EstadoInventario.DISPONIBLE);
        inventory.setTipoManejo(TipoManejoInventario.STOCK_EXACTO);
        return inventory;
    }
}
