package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.recred.backend.domain.models.EstadoCompra.EN_PREPARACION;
import static com.recred.backend.domain.models.EstadoCompra.LISTO;
import static com.recred.backend.domain.models.EstadoCompra.PENDIENTE;
import static com.recred.backend.domain.models.EstadoCompra.VENCIDO;

public class ExpireAdvancePurchases {

    private static final List<EstadoCompra> ACTIVE_ADVANCE_STATUSES = List.of(
            PENDIENTE,
            EN_PREPARACION,
            LISTO
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
    public ExpireAdvancePurchasesResponse execute(ExpireAdvancePurchasesRequest request) {
        return expire(expiredPurchases(request));
    }

    private List<Purchase> expiredPurchases(ExpireAdvancePurchasesRequest request) {
        if (request.buffetId() == null) {
            return purchaseRepository.findExpiredAdvancePurchases(
                    request.targetTime().toLocalDate(),
                    request.targetTime().toLocalTime(),
                    ACTIVE_ADVANCE_STATUSES);
        }

        return purchaseRepository.findExpiredAdvancePurchases(
                request.buffetId(),
                request.targetTime().toLocalDate(),
                request.targetTime().toLocalTime(),
                ACTIVE_ADVANCE_STATUSES);
    }

    private ExpireAdvancePurchasesResponse expire(List<Purchase> purchases) {
        int releasedReservations = purchases.stream()
                .mapToInt(this::expire)
                .sum();
        return new ExpireAdvancePurchasesResponse(purchases.size(), releasedReservations);
    }

    private int expire(Purchase purchase) {
        EstadoCompra previousStatus = purchase.getStatus();
        purchase.changeStatusTo(VENCIDO);
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
        return purchase.shouldReleaseReservationWhenChangingFrom(
                previousStatus,
                VENCIDO);
    }
}
