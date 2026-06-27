package com.recred.backend.unit.domain.models;

import com.recred.backend.domain.models.MovimientoStock;
import com.recred.backend.domain.models.TipoMovimientoStock;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MovimientoStockTest {

    @Test
    void isRelease_ShouldIdentifyReleasedReservations() {
        MovimientoStock release = movement(TipoMovimientoStock.LIBERACION, 3, 1);
        MovimientoStock sale = movement(TipoMovimientoStock.VENTA, 2, 0);

        assertEquals(true, release.isRelease());
        assertEquals(false, sale.isRelease());
    }

    @Test
    void quantityOrZero_ShouldNormalizeNullQuantities() {
        assertEquals(0, movement(TipoMovimientoStock.LIBERACION, null, 1).quantityOrZero());
        assertEquals(3, movement(TipoMovimientoStock.LIBERACION, 3, 1).quantityOrZero());
    }

    @Test
    void reachedZeroStock_ShouldIdentifyMovementsThatLeaveNoStock() {
        assertEquals(true, movement(TipoMovimientoStock.VENTA, 2, 0).reachedZeroStock());
        assertEquals(true, movement(TipoMovimientoStock.AJUSTE, 2, -1).reachedZeroStock());
        assertEquals(false, movement(TipoMovimientoStock.AJUSTE, 2, 1).reachedZeroStock());
        assertEquals(false, movement(TipoMovimientoStock.AJUSTE, 2, null).reachedZeroStock());
    }

    @Test
    void hasInventoryId_ShouldReturnFalseWhenInventoryIdIsNull() {
        MovimientoStock movement = new MovimientoStock(
                UUID.randomUUID(),
                null,
                TipoMovimientoStock.AJUSTE,
                1,
                2,
                1,
                "Ajuste",
                null,
                null,
                LocalDateTime.now()
        );

        assertEquals(false, movement.hasInventoryId());
    }

    private MovimientoStock movement(
            TipoMovimientoStock type,
            Integer quantity,
            Integer newQuantity
    ) {
        return new MovimientoStock(
                UUID.randomUUID(),
                UUID.randomUUID(),
                type,
                quantity,
                4,
                newQuantity,
                "Movimiento operativo",
                null,
                null,
                LocalDateTime.now()
        );
    }
}
