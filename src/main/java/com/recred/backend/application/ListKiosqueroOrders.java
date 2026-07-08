package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;

import java.util.List;
import java.util.UUID;

public class ListKiosqueroOrders {

    private static final int DEFAULT_LIMIT = 100;

    private final PurchaseRepository purchaseRepository;

    public ListKiosqueroOrders(PurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    public ListKiosqueroOrdersResponse execute(ListKiosqueroOrdersRequest listOrdersRequest) {
        requireRequest(listOrdersRequest);
        requireBuffetId(listOrdersRequest.buffetId());
        return ListKiosqueroOrdersResponse.from(findKiosqueroOrders(listOrdersRequest));
    }

    private List<Purchase> findKiosqueroOrders(
            ListKiosqueroOrdersRequest request
    ) {
        return purchaseRepository.findKiosqueroOrders(
                request.buffetId(),
                normalizedStatuses(request.statuses()),
                request.date(),
                request.pickupSlotId(),
                normalizedLimit(request.limit())
        );
    }

    private void requireRequest(ListKiosqueroOrdersRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
    }

    private void requireBuffetId(UUID buffetId) {
        if (buffetId == null) {
            throw new IllegalArgumentException("buffetId is required");
        }
    }

    private List<EstadoCompra> normalizedStatuses(List<EstadoCompra> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return EstadoCompra.activeTrackingStatuses();
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
