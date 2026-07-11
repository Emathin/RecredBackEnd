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

    private final UUID BUFFET_ID = UUID.randomUUID();
    private final UUID PRODUCT_ID = UUID.randomUUID();
    private final UUID INVENTORY_ID = UUID.randomUUID();
    private final UUID PURCHASE_ID = UUID.randomUUID();
    private final LocalDate DATE = LocalDate.of(2026, 6, 15);
    private final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 6, 15, 12, 0);

    @Test
    void fromDomainShouldMapInventoryPayload() {
        KiosqueroRealtimeEvent event = givenInventoryEvent();
        
        KiosqueroRealtimeEventResponse response = whenMappingFromDomain(event);
        
        thenInventoryIsMapped(response);
    }

    @Test
    void fromDomainShouldMapPurchasePayload() {
        KiosqueroRealtimeEvent event = givenPurchaseEvent();
        
        KiosqueroRealtimeEventResponse response = whenMappingFromDomain(event);
        
        thenPurchaseIsMapped(response);
    }

    @Test
    void fromDomainShouldHandleNullableFields() {
        KiosqueroRealtimeEvent event = givenAlertEvent();
        
        KiosqueroRealtimeEventResponse response = whenMappingFromDomain(event);
        
        thenNullableFieldsAreHandled(response);
    }

    private KiosqueroRealtimeEvent givenInventoryEvent() {
        return new KiosqueroRealtimeEvent(
                BUFFET_ID,
                KiosqueroRealtimeEventType.STOCK_CHANGED,
                KiosqueroRealtimeChangeKind.RESERVATION,
                "Reserva",
                DATE,
                OCCURRED_AT,
                "Stock actualizado",
                new InventoryRealtimePayload(
                        PRODUCT_ID,
                        INVENTORY_ID,
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
    }

    private KiosqueroRealtimeEvent givenPurchaseEvent() {
        return new KiosqueroRealtimeEvent(
                BUFFET_ID,
                KiosqueroRealtimeEventType.ORDER_UPDATED,
                KiosqueroRealtimeChangeKind.ORDER_CHANGED,
                "Pedido actualizado",
                DATE,
                OCCURRED_AT,
                "Pedido listo",
                new PurchaseRealtimePayload(
                        PURCHASE_ID,
                        EstadoCompra.LISTO,
                        new BigDecimal("120.00")
                )
        );
    }

    private KiosqueroRealtimeEvent givenAlertEvent() {
        return KiosqueroRealtimeEvent.productAlert(
                BUFFET_ID,
                KiosqueroRealtimeEventType.LOW_STOCK,
                PRODUCT_ID,
                "Stock bajo"
        );
    }

    private KiosqueroRealtimeEventResponse whenMappingFromDomain(KiosqueroRealtimeEvent event) {
        return KiosqueroRealtimeEventResponse.fromDomain(event);
    }

    private void thenInventoryIsMapped(KiosqueroRealtimeEventResponse response) {
        assertEquals(BUFFET_ID, response.buffetId());
        assertEquals("STOCK_CHANGED", response.type());
        assertEquals(PRODUCT_ID, response.productId());
        assertEquals(INVENTORY_ID, response.inventoryId());
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
        assertEquals(DATE, response.date());
        assertEquals(OCCURRED_AT, response.occurredAt());
        assertEquals("Stock actualizado", response.message());
        assertNull(response.purchaseId());
        assertNull(response.purchaseStatus());
        assertNull(response.purchaseTotal());
    }

    private void thenPurchaseIsMapped(KiosqueroRealtimeEventResponse response) {
        assertEquals(BUFFET_ID, response.buffetId());
        assertEquals("ORDER_UPDATED", response.type());
        assertEquals(PURCHASE_ID, response.purchaseId());
        assertEquals("LISTO", response.purchaseStatus());
        assertEquals(new BigDecimal("120.00"), response.purchaseTotal());
        assertEquals("ORDER_CHANGED", response.changeKind());
        assertEquals("Pedido actualizado", response.reason());
        assertEquals(DATE, response.date());
        assertEquals(OCCURRED_AT, response.occurredAt());
        assertEquals("Pedido listo", response.message());
        assertNull(response.productId());
        assertNull(response.inventoryId());
        assertNull(response.movementType());
        assertNull(response.estadoInventario());
        assertNull(response.tipoManejoInventario());
    }

    private void thenNullableFieldsAreHandled(KiosqueroRealtimeEventResponse response) {
        assertEquals(BUFFET_ID, response.buffetId());
        assertEquals("LOW_STOCK", response.type());
        assertEquals(PRODUCT_ID, response.productId());
        assertNull(response.purchaseStatus());
        assertNull(response.movementType());
        assertNull(response.estadoInventario());
        assertNull(response.tipoManejoInventario());
        assertNull(response.changeKind());
        assertEquals("Stock bajo", response.message());
    }
}
