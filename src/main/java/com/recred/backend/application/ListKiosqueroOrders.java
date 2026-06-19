package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class ListKiosqueroOrders {

    private static final int DEFAULT_LIMIT = 100;
    private static final List<EstadoCompra> ACTIVE_STATUSES = List.of(
            EstadoCompra.PENDIENTE,
            EstadoCompra.EN_PREPARACION,
            EstadoCompra.LISTO
    );

    private final PurchaseRepository purchaseRepository;

    public ListKiosqueroOrders(PurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    public List<Purchase> execute(
            UUID buffetId,
            List<EstadoCompra> statuses,
            LocalDate date,
            UUID pickupSlotId,
            Integer limit
    ) {
        if (buffetId == null) {
            throw new IllegalArgumentException("buffetId is required");
        }

        return purchaseRepository.findKiosqueroOrders(
                buffetId,
                normalizedStatuses(statuses),
                date,
                pickupSlotId,
                normalizedLimit(limit)
        );
    }

    private List<EstadoCompra> normalizedStatuses(List<EstadoCompra> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return ACTIVE_STATUSES;
        }

        return statuses;
    }

    private int normalizedLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }

        return Math.min(limit, DEFAULT_LIMIT);
    }
}
