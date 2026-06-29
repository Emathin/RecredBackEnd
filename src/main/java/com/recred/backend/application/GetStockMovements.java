package com.recred.backend.application;

import com.recred.backend.domain.exceptions.InventoryNotFoundException;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.domain.repositories.StockMovementRepository;
import java.util.UUID;

public class GetStockMovements {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;

    public GetStockMovements(
            InventoryRepository inventoryRepository,
            StockMovementRepository stockMovementRepository
    ) {
        this.inventoryRepository = inventoryRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    public GetStockMovementsResponse execute(GetStockMovementsRequest request) {
        validate(request);
        UUID inventoryId = inventoryId(request);
        return GetStockMovementsResponse.fromDomain(
                stockMovementRepository.findByInventoryId(inventoryId));
    }

    private void validate(GetStockMovementsRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }

        if (request.buffetId() == null) {
            throw new IllegalArgumentException("buffetId is required");
        }

        if (request.productId() == null) {
            throw new IllegalArgumentException("productId is required");
        }
    }

    private UUID inventoryId(GetStockMovementsRequest request) {
        return inventoryRepository.findByBuffetAndProduct(request.buffetId(), request.productId())
                .orElseThrow(() -> new InventoryNotFoundException(
                        request.buffetId(),
                        request.productId()))
                .getId();
    }
}
