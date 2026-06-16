package com.recred.backend.domain.services;

import com.recred.backend.domain.events.KiosqueroRealtimeEvent;
import com.recred.backend.domain.events.KiosqueroRealtimeEventPublisher;
import com.recred.backend.domain.events.KiosqueroRealtimeEventType;
import com.recred.backend.domain.exceptions.PickupCutoffTimeExceededException;
import com.recred.backend.domain.exceptions.PickupSlotCapacityExceededException;
import com.recred.backend.domain.exceptions.ProductDailyCapacityExceededException;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdvancePurchaseRulesServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    @Test
    void validate_WhenBeforeCutoffAndCapacityAvailable_ShouldPass() {
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        UUID buffetId = UUID.randomUUID();
        FranjaHoraria slot = slot(2, 15);
        AdvancePurchaseRulesService service = service(purchaseRepository,
                "2026-05-27T12:30:00Z");

        when(purchaseRepository.countScheduledAdvancePurchases(
                eq(buffetId),
                eq(slot.getId()),
                eq(LocalDate.of(2026, 5, 27)),
                anyList()
        )).thenReturn(1L);

        assertDoesNotThrow(() -> service.validate(buffetId, slot,
                LocalDate.of(2026, 5, 27)));
    }

    @Test
    void validate_WhenCutoffWasReached_ShouldThrowException() {
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        FranjaHoraria slot = slot(2, 15);
        AdvancePurchaseRulesService service = service(purchaseRepository,
                "2026-05-27T12:50:00Z");

        assertThrows(PickupCutoffTimeExceededException.class,
                () -> service.validate(UUID.randomUUID(), slot,
                        LocalDate.of(2026, 5, 27)));
    }

    @Test
    void validate_WhenCapacityIsFull_ShouldThrowException() {
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        UUID buffetId = UUID.randomUUID();
        FranjaHoraria slot = slot(2, 0);
        AdvancePurchaseRulesService service = service(purchaseRepository,
                "2026-05-27T12:00:00Z");

        when(purchaseRepository.countScheduledAdvancePurchases(
                eq(buffetId),
                eq(slot.getId()),
                eq(LocalDate.of(2026, 5, 27)),
                anyList()
        )).thenReturn(2L);

        assertThrows(PickupSlotCapacityExceededException.class,
                () -> service.validate(buffetId, slot,
                        LocalDate.of(2026, 5, 27)));
    }

    @Test
    void validate_WhenCapacityIsUnlimited_ShouldNotCountScheduledPurchases() {
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        UUID buffetId = UUID.randomUUID();
        FranjaHoraria slot = slot(null, 0);
        AdvancePurchaseRulesService service = service(purchaseRepository,
                "2026-05-27T12:00:00Z");

        service.validate(buffetId, slot, LocalDate.of(2026, 5, 27));

        verify(purchaseRepository, never()).countScheduledAdvancePurchases(
                eq(buffetId),
                eq(slot.getId()),
                eq(LocalDate.of(2026, 5, 27)),
                anyList());
    }

    @Test
    void validate_WhenProductDailyCapacityIsFull_ShouldThrowException() {
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        UUID buffetId = UUID.randomUUID();
        AdvancePurchaseRulesService service = service(purchaseRepository,
                "2026-05-27T12:00:00Z");
        Producto product = preparedProduct();
        product.setCupoMaximoDiario(3);

        when(purchaseRepository.sumScheduledProductQuantity(
                eq(buffetId),
                eq(product.getId()),
                eq(LocalDate.of(2026, 5, 27)),
                anyList()
        )).thenReturn(2L);

        assertThrows(ProductDailyCapacityExceededException.class,
                () -> service.validate(
                        buffetId,
                        slot(null, 0),
                        LocalDate.of(2026, 5, 27),
                        Map.of(product, 2)));
    }

    @Test
    void validate_WhenCartIsNullOrEmpty_ShouldSkipProductCapacityRules() {
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        UUID buffetId = UUID.randomUUID();
        FranjaHoraria slot = slot(null, 0);
        AdvancePurchaseRulesService service = service(purchaseRepository,
                "2026-05-27T12:00:00Z");

        service.validate(buffetId, slot, LocalDate.of(2026, 5, 27), null);
        service.validate(buffetId, slot, LocalDate.of(2026, 5, 27), Map.of());

        verify(purchaseRepository, never()).sumScheduledProductQuantity(
                eq(buffetId),
                org.mockito.ArgumentMatchers.any(),
                eq(LocalDate.of(2026, 5, 27)),
                anyList()
        );
    }

    @Test
    void validate_WhenProductHasNoDailyCapacityRule_ShouldSkipProductCapacity() {
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        UUID buffetId = UUID.randomUUID();
        AdvancePurchaseRulesService service = service(purchaseRepository,
                "2026-05-27T12:00:00Z");
        Producto product = preparedProduct();
        product.setTipoManejoInventario(null);

        service.validate(
                buffetId,
                slot(null, 0),
                LocalDate.of(2026, 5, 27),
                Map.of(product, 1)
        );

        verify(purchaseRepository, never()).sumScheduledProductQuantity(
                eq(buffetId),
                eq(product.getId()),
                eq(LocalDate.of(2026, 5, 27)),
                anyList()
        );
    }

    @Test
    void validate_WhenDailyCapacityLimitIsMissing_ShouldSkipProductCapacity() {
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        UUID buffetId = UUID.randomUUID();
        AdvancePurchaseRulesService service = service(purchaseRepository,
                "2026-05-27T12:00:00Z");
        Producto product = preparedProduct();
        product.setCupoMaximoDiario(null);

        service.validate(
                buffetId,
                slot(null, 0),
                LocalDate.of(2026, 5, 27),
                Map.of(product, 1)
        );

        verify(purchaseRepository, never()).sumScheduledProductQuantity(
                eq(buffetId),
                eq(product.getId()),
                eq(LocalDate.of(2026, 5, 27)),
                anyList()
        );
    }

    @Test
    void validate_WhenRequestedQuantityIsNull_ShouldTreatItAsZero() {
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        UUID buffetId = UUID.randomUUID();
        AdvancePurchaseRulesService service = service(purchaseRepository,
                "2026-05-27T12:00:00Z");
        Producto product = preparedProduct();
        product.setCupoMaximoDiario(10);

        when(purchaseRepository.sumScheduledProductQuantity(
                eq(buffetId),
                eq(product.getId()),
                eq(LocalDate.of(2026, 5, 27)),
                anyList()
        )).thenReturn(5L);

        assertDoesNotThrow(() -> service.validate(
                buffetId,
                slot(null, 0),
                LocalDate.of(2026, 5, 27),
                java.util.Collections.singletonMap(product, null)
        ));
    }

    @Test
    void validate_WhenDailyCapacityIsNotLow_ShouldNotPublishRealtimeEvent() {
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        KiosqueroRealtimeEventPublisher publisher =
                mock(KiosqueroRealtimeEventPublisher.class);
        UUID buffetId = UUID.randomUUID();
        Producto product = preparedProduct();
        product.setCupoMaximoDiario(10);
        AdvancePurchaseRulesService service = service(
                purchaseRepository,
                publisher,
                "2026-05-27T12:00:00Z"
        );
        when(purchaseRepository.sumScheduledProductQuantity(
                eq(buffetId),
                eq(product.getId()),
                eq(LocalDate.of(2026, 5, 27)),
                anyList()
        )).thenReturn(2L);

        service.validate(
                buffetId,
                slot(null, 0),
                LocalDate.of(2026, 5, 27),
                Map.of(product, 1)
        );

        verify(publisher, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validate_WhenUsingDefaultPublisherAndCapacityIsLow_ShouldNotThrow() {
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        UUID buffetId = UUID.randomUUID();
        Producto product = preparedProduct();
        product.setCupoMaximoDiario(10);
        AdvancePurchaseRulesService service = service(purchaseRepository,
                "2026-05-27T12:00:00Z");
        when(purchaseRepository.sumScheduledProductQuantity(
                eq(buffetId),
                eq(product.getId()),
                eq(LocalDate.of(2026, 5, 27)),
                anyList()
        )).thenReturn(7L);

        assertDoesNotThrow(() -> service.validate(
                buffetId,
                slot(null, 0),
                LocalDate.of(2026, 5, 27),
                Map.of(product, 1)
        ));
    }

    @Test
    void validate_WhenRemainingDailyCapacityIsLow_ShouldPublishRealtimeEvent() {
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        KiosqueroRealtimeEventPublisher publisher =
                mock(KiosqueroRealtimeEventPublisher.class);
        UUID buffetId = UUID.randomUUID();
        Producto product = preparedProduct();
        product.setCupoMaximoDiario(10);
        AdvancePurchaseRulesService service = service(
                purchaseRepository,
                publisher,
                "2026-05-27T12:00:00Z"
        );
        when(purchaseRepository.sumScheduledProductQuantity(
                eq(buffetId),
                eq(product.getId()),
                eq(LocalDate.of(2026, 5, 27)),
                anyList()
        )).thenReturn(7L);

        service.validate(
                buffetId,
                slot(null, 0),
                LocalDate.of(2026, 5, 27),
                Map.of(product, 1)
        );

        ArgumentCaptor<KiosqueroRealtimeEvent> captor =
                ArgumentCaptor.forClass(KiosqueroRealtimeEvent.class);
        verify(publisher).publish(captor.capture());
        assertEquals(KiosqueroRealtimeEventType.DAILY_CAPACITY_LOW,
                captor.getValue().type());
        assertEquals(product.getId(), captor.getValue().productId());
    }

    private AdvancePurchaseRulesService service(
            PurchaseRepository purchaseRepository,
            String instant
    ) {
        return new AdvancePurchaseRulesService(
                purchaseRepository,
                Clock.fixed(Instant.parse(instant), ZONE)
        );
    }

    private AdvancePurchaseRulesService service(
            PurchaseRepository purchaseRepository,
            KiosqueroRealtimeEventPublisher publisher,
            String instant
    ) {
        return new AdvancePurchaseRulesService(
                purchaseRepository,
                Clock.fixed(Instant.parse(instant), ZONE),
                publisher
        );
    }

    private FranjaHoraria slot(Integer capacity, Integer cutoffMinutes) {
        FranjaHoraria slot = new FranjaHoraria();
        slot.setId(UUID.randomUUID());
        slot.setHoraInicio(LocalTime.of(10, 0));
        slot.setHoraFin(LocalTime.of(10, 20));
        slot.setCupoMaximo(capacity);
        slot.setMinutosCorte(cutoffMinutes);
        return slot;
    }

    private Producto preparedProduct() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Sandwich");
        product.setTipoManejoInventario(TipoManejoInventario.CUPO_DIARIO);
        return product;
    }
}
