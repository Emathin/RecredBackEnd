package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ExpireAdvancePurchases {

    private static final List<EstadoCompra> ACTIVE_ADVANCE_STATUSES = List.of(
            EstadoCompra.PENDIENTE,
            EstadoCompra.EN_PREPARACION,
            EstadoCompra.LISTO
    );

    private final PurchaseRepository purchaseRepository;
    private final InventoryAvailabilityService inventoryAvailabilityService;

    public ExpireAdvancePurchases(
            PurchaseRepository purchaseRepository,
            InventoryAvailabilityService inventoryAvailabilityService
    ) {
        this.purchaseRepository = purchaseRepository;
        this.inventoryAvailabilityService = inventoryAvailabilityService;
    }

    @Transactional
    public int execute(LocalDateTime now) {
        return executeDetailed(now).expiredPurchases();
    }

    @Transactional
    public ExpirationResult executeDetailed(LocalDateTime now) {
        return expire(purchaseRepository.findExpiredAdvancePurchases(
                now.toLocalDate(),
                now.toLocalTime(),
                ACTIVE_ADVANCE_STATUSES));
    }

    @Transactional
    public int execute(UUID buffetId, LocalDateTime now) {
        return executeDetailed(buffetId, now).expiredPurchases();
    }

    @Transactional
    public ExpirationResult executeDetailed(UUID buffetId, LocalDateTime now) {
        return expire(purchaseRepository.findExpiredAdvancePurchases(
                buffetId,
                now.toLocalDate(),
                now.toLocalTime(),
                ACTIVE_ADVANCE_STATUSES));
    }

    private ExpirationResult expire(List<Purchase> purchases) {
        int releasedReservations = purchases.stream()
                .mapToInt(this::expire)
                .sum();
        return new ExpirationResult(purchases.size(), releasedReservations);
    }

    private int expire(Purchase purchase) {
        EstadoCompra previousStatus = purchase.getStatus();
        purchase.changeStatusTo(EstadoCompra.VENCIDO);
        int releasedReservations = 0;

        if (shouldReleaseReservation(purchase, previousStatus)) {
            releasedReservations = purchase.getItems().values().stream()
                    .filter(quantity -> quantity != null)
                    .reduce(0, Integer::sum);
            inventoryAvailabilityService.releaseReservation(
                    purchase.getBuffetId(),
                    purchase.getItems(),
                    purchase.getId()
            );
        }

        purchaseRepository.save(purchase);
        return releasedReservations;
    }

    private boolean shouldReleaseReservation(Purchase purchase, EstadoCompra previousStatus) {
        return purchase.getTipo() == TipoCompra.ANTICIPADA
                && (previousStatus == EstadoCompra.PENDIENTE
                || previousStatus == EstadoCompra.EN_PREPARACION
                || previousStatus == EstadoCompra.LISTO);
    }

    public record ExpirationResult(int expiredPurchases, int releasedReservations) {
    }
}
