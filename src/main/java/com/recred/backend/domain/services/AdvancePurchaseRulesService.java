package com.recred.backend.domain.services;

import com.recred.backend.domain.events.KiosqueroRealtimeEvent;
import com.recred.backend.domain.events.KiosqueroRealtimeEventPublisher;
import com.recred.backend.domain.events.KiosqueroRealtimeEventType;
import com.recred.backend.domain.exceptions.PickupCutoffTimeExceededException;
import com.recred.backend.domain.exceptions.PickupSlotCapacityExceededException;
import com.recred.backend.domain.exceptions.ProductDailyCapacityExceededException;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.repositories.PurchaseRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AdvancePurchaseRulesService {

    private static final List<EstadoCompra> ACTIVE_ADVANCE_STATUSES = List.of(
            EstadoCompra.PENDIENTE,
            EstadoCompra.EN_PREPARACION,
            EstadoCompra.LISTO
    );

    private final PurchaseRepository purchaseRepository;
    private final Clock clock;
    private final KiosqueroRealtimeEventPublisher realtimeEventPublisher;

    public AdvancePurchaseRulesService(PurchaseRepository purchaseRepository, Clock clock) {
        this(purchaseRepository, clock, event -> {
        });
    }

    public AdvancePurchaseRulesService(
            PurchaseRepository purchaseRepository,
            Clock clock,
            KiosqueroRealtimeEventPublisher realtimeEventPublisher
    ) {
        this.purchaseRepository = purchaseRepository;
        this.clock = clock;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    public void validate(UUID buffetId, FranjaHoraria pickupSlot, LocalDate pickupDate) {
        validateCutoff(pickupSlot, pickupDate);
        validateCapacity(buffetId, pickupSlot, pickupDate);
    }

    public void validate(
            UUID buffetId,
            FranjaHoraria pickupSlot,
            LocalDate pickupDate,
            Map<Producto, Integer> cart
    ) {
        validate(buffetId, pickupSlot, pickupDate);
        validateProductCapacityRules(buffetId, pickupDate, cart);
    }

    private void validateCutoff(FranjaHoraria pickupSlot, LocalDate pickupDate) {
        LocalDateTime cutoffTime = LocalDateTime.of(pickupDate, pickupSlot.getHoraInicio())
                .minusMinutes(pickupSlot.minutosCorteSeguro());

        if (!LocalDateTime.now(clock).isBefore(cutoffTime)) {
            throw new PickupCutoffTimeExceededException(pickupSlot.getId(), cutoffTime);
        }
    }

    private void validateCapacity(UUID buffetId, FranjaHoraria pickupSlot, LocalDate pickupDate) {
        if (!pickupSlot.tieneCupoLimitado()) {
            return;
        }

        long scheduledPurchases = purchaseRepository.countScheduledAdvancePurchases(
                buffetId,
                pickupSlot.getId(),
                pickupDate,
                ACTIVE_ADVANCE_STATUSES);

        if (scheduledPurchases >= pickupSlot.getCupoMaximo()) {
            throw new PickupSlotCapacityExceededException(
                    pickupSlot.getId(),
                    pickupDate,
                    pickupSlot.getCupoMaximo());
        }
    }

    private void validateProductCapacityRules(
            UUID buffetId,
            LocalDate pickupDate,
            Map<Producto, Integer> cart
    ) {
        if (cart == null || cart.isEmpty()) {
            return;
        }

        cart.forEach((product, quantity) -> validateProductDailyCapacity(
                buffetId,
                pickupDate,
                product,
                quantity
        ));
    }

    private void validateProductDailyCapacity(
            UUID buffetId,
            LocalDate pickupDate,
            Producto product,
            Integer quantity
    ) {
        if (product == null
                || product.getTipoManejoInventario() != TipoManejoInventario.CUPO_DIARIO) {
            return;
        }

        if (product.getCupoMaximoDiario() == null || product.getCupoMaximoDiario() <= 0) {
            return;
        }

        long reserved = purchaseRepository.sumScheduledProductQuantity(
                buffetId,
                product.getId(),
                pickupDate,
                ACTIVE_ADVANCE_STATUSES);
        int requested = quantity == null ? 0 : quantity;
        int remaining = product.getCupoMaximoDiario() - (int) reserved;
        if (requested > remaining) {
            throw new ProductDailyCapacityExceededException(
                    product.getId(),
                    pickupDate,
                    product.getCupoMaximoDiario());
        }

        if (isLowDailyCapacity(product.getCupoMaximoDiario(), remaining - requested)) {
            publishDailyCapacityLow(buffetId, product);
        }
    }

    private boolean isLowDailyCapacity(int capacity, int remaining) {
        return remaining >= 0 && remaining <= Math.max(1, capacity / 5);
    }

    private void publishDailyCapacityLow(UUID buffetId, Producto product) {
        realtimeEventPublisher.publish(KiosqueroRealtimeEvent.productAlert(
                buffetId,
                KiosqueroRealtimeEventType.DAILY_CAPACITY_LOW,
                product.getId(),
                "Cupo diario bajo para " + product.getNombre()
        ));
    }
}
