package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.events.KiosqueroRealtimeChangeKind;
import com.recred.backend.domain.events.KiosqueroRealtimeEvent;
import com.recred.backend.domain.events.KiosqueroRealtimeEventType;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.models.TipoMovimientoStock;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KiosqueroRealtimeEventResponseTest {

    @Test
    void fromDomainShouldMapAllEnumValuesAsNames() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID inventoryId = UUID.randomUUID();
        UUID purchaseId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 15);
        LocalDateTime occurredAt = LocalDateTime.of(2026, 6, 15, 12, 0);
        KiosqueroRealtimeEvent event = new KiosqueroRealtimeEvent(
                buffetId,
                KiosqueroRealtimeEventType.STOCK_CHANGED,
                productId,
                inventoryId,
                purchaseId,
                EstadoCompra.LISTO,
                new BigDecimal("120.00"),
                TipoMovimientoStock.RESERVA,
                10,
                3,
                7,
                2,
                20,
                15,
                true,
                EstadoInventario.DISPONIBLE,
                TipoManejoInventario.CUPO_DIARIO,
                KiosqueroRealtimeChangeKind.RESERVATION,
                "Reserva",
                date,
                occurredAt,
                "Stock actualizado"
        );

        KiosqueroRealtimeEventResponse response = KiosqueroRealtimeEventResponse
                .fromDomain(event);

        assertEquals(buffetId, response.buffetId());
        assertEquals("STOCK_CHANGED", response.type());
        assertEquals(productId, response.productId());
        assertEquals(inventoryId, response.inventoryId());
        assertEquals(purchaseId, response.purchaseId());
        assertEquals("LISTO", response.purchaseStatus());
        assertEquals(new BigDecimal("120.00"), response.purchaseTotal());
        assertEquals("RESERVA", response.movementType());
        assertEquals(10, response.stockActual());
        assertEquals(3, response.stockReservado());
        assertEquals(7, response.stockDisponible());
        assertEquals(2, response.stockMinimo());
        assertEquals(20, response.cupoMaximoDiario());
        assertEquals(15, response.cupoDisponibleDia());
        assertEquals(true, response.disponible());
        assertEquals("DISPONIBLE", response.estadoInventario());
        assertEquals("CUPO_DIARIO", response.tipoManejoInventario());
        assertEquals("RESERVATION", response.changeKind());
        assertEquals("Reserva", response.reason());
        assertEquals(date, response.date());
        assertEquals(occurredAt, response.occurredAt());
        assertEquals("Stock actualizado", response.message());
    }

    @Test
    void fromDomainShouldHandleNullableFields() {
        UUID buffetId = UUID.randomUUID();
        KiosqueroRealtimeEvent event = KiosqueroRealtimeEvent.productAlert(
                buffetId,
                KiosqueroRealtimeEventType.LOW_STOCK,
                UUID.randomUUID(),
                "Stock bajo"
        );

        KiosqueroRealtimeEventResponse response = KiosqueroRealtimeEventResponse
                .fromDomain(event);

        assertEquals(buffetId, response.buffetId());
        assertEquals("LOW_STOCK", response.type());
        assertNull(response.purchaseStatus());
        assertNull(response.movementType());
        assertNull(response.estadoInventario());
        assertNull(response.tipoManejoInventario());
        assertNull(response.changeKind());
        assertEquals("Stock bajo", response.message());
    }
}
