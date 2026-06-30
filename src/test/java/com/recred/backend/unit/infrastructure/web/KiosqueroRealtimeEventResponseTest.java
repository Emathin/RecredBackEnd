package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.KiosqueroRealtimeEventResponse;
import com.recred.backend.domain.events.InventoryRealtimePayload;
import com.recred.backend.domain.events.KiosqueroRealtimeChangeKind;
import com.recred.backend.domain.events.KiosqueroRealtimeEvent;
import com.recred.backend.domain.events.KiosqueroRealtimeEventType;
import com.recred.backend.domain.events.PurchaseRealtimePayload;
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
    void fromDomainShouldMapInventoryPayload() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID inventoryId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 15);
        LocalDateTime occurredAt = LocalDateTime.of(2026, 6, 15, 12, 0);
        KiosqueroRealtimeEvent event = new KiosqueroRealtimeEvent(
                buffetId,
                KiosqueroRealtimeEventType.STOCK_CHANGED,
                KiosqueroRealtimeChangeKind.RESERVATION,
                "Reserva",
                date,
                occurredAt,
                "Stock actualizado",
                new InventoryRealtimePayload(
                        productId,
                        inventoryId,
                        TipoMovimientoStock.RESERVA,
                        10,
                        3,
                        7,
                        2,
                        20,
                        15,
                        true,
                        EstadoInventario.DISPONIBLE,
                        TipoManejoInventario.CUPO_DIARIO
                )
        );

        KiosqueroRealtimeEventResponse response = KiosqueroRealtimeEventResponse
                .fromDomain(event);

        assertEquals(buffetId, response.buffetId());
        assertEquals("STOCK_CHANGED", response.type());
        assertEquals(productId, response.productId());
        assertEquals(inventoryId, response.inventoryId());
        assertEquals("RESERVA", response.movementType());
        assertEquals(10, response.stockActual());
        assertEquals(3, response.stockReservado());
        assertEquals(7, response.stockDisponible());
        assertEquals(2, response.stockMinimo());
        assertEquals(20, response.cupoMaximoDiario());
        assertEquals(15, response.cupoDisponibleDia());
        assertEquals(Boolean.TRUE, response.disponible());
        assertEquals("DISPONIBLE", response.estadoInventario());
        assertEquals("CUPO_DIARIO", response.tipoManejoInventario());
        assertEquals("RESERVATION", response.changeKind());
        assertEquals("Reserva", response.reason());
        assertEquals(date, response.date());
        assertEquals(occurredAt, response.occurredAt());
        assertEquals("Stock actualizado", response.message());
        assertNull(response.purchaseId());
        assertNull(response.purchaseStatus());
        assertNull(response.purchaseTotal());
    }

    @Test
    void fromDomainShouldMapPurchasePayload() {
        UUID buffetId = UUID.randomUUID();
        UUID purchaseId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 15);
        LocalDateTime occurredAt = LocalDateTime.of(2026, 6, 15, 12, 0);
        BigDecimal purchaseTotal = new BigDecimal("120.00");
        KiosqueroRealtimeEvent event = new KiosqueroRealtimeEvent(
                buffetId,
                KiosqueroRealtimeEventType.ORDER_UPDATED,
                KiosqueroRealtimeChangeKind.ORDER_CHANGED,
                "Pedido actualizado",
                date,
                occurredAt,
                "Pedido listo",
                new PurchaseRealtimePayload(
                        purchaseId,
                        EstadoCompra.LISTO,
                        purchaseTotal
                )
        );

        KiosqueroRealtimeEventResponse response = KiosqueroRealtimeEventResponse
                .fromDomain(event);

        assertEquals(buffetId, response.buffetId());
        assertEquals("ORDER_UPDATED", response.type());
        assertEquals(purchaseId, response.purchaseId());
        assertEquals("LISTO", response.purchaseStatus());
        assertEquals(purchaseTotal, response.purchaseTotal());
        assertEquals("ORDER_CHANGED", response.changeKind());
        assertEquals("Pedido actualizado", response.reason());
        assertEquals(date, response.date());
        assertEquals(occurredAt, response.occurredAt());
        assertEquals("Pedido listo", response.message());
        assertNull(response.productId());
        assertNull(response.inventoryId());
        assertNull(response.movementType());
        assertNull(response.estadoInventario());
        assertNull(response.tipoManejoInventario());
    }

    @Test
    void fromDomainShouldHandleNullableFields() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        KiosqueroRealtimeEvent event = KiosqueroRealtimeEvent.productAlert(
                buffetId,
                KiosqueroRealtimeEventType.LOW_STOCK,
                productId,
                "Stock bajo"
        );

        KiosqueroRealtimeEventResponse response = KiosqueroRealtimeEventResponse
                .fromDomain(event);

        assertEquals(buffetId, response.buffetId());
        assertEquals("LOW_STOCK", response.type());
        assertEquals(productId, response.productId());
        assertNull(response.purchaseStatus());
        assertNull(response.movementType());
        assertNull(response.estadoInventario());
        assertNull(response.tipoManejoInventario());
        assertNull(response.changeKind());
        assertEquals("Stock bajo", response.message());
    }
}

