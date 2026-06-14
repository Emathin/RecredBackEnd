package com.recred.backend.domain.services;

import com.recred.backend.domain.exceptions.PickupCutoffTimeExceededException;
import com.recred.backend.domain.exceptions.PickupSlotCapacityExceededException;
import com.recred.backend.domain.exceptions.ProductDailyCapacityExceededException;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

    private AdvancePurchaseRulesService service(
            PurchaseRepository purchaseRepository,
            String instant
    ) {
        return new AdvancePurchaseRulesService(
                purchaseRepository,
                Clock.fixed(Instant.parse(instant), ZONE)
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
