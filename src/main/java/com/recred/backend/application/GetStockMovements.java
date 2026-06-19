package com.recred.backend.application;

import com.recred.backend.domain.exceptions.InventoryNotFoundException;
import com.recred.backend.domain.models.MovimientoStock;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.domain.repositories.StockMovementRepository;
import java.util.List;
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

    public List<MovimientoStock> execute(UUID buffetId, UUID productId) {
        if (buffetId == null) {
            throw new IllegalArgumentException("buffetId is required");
        }

        if (productId == null) {
            throw new IllegalArgumentException("productId is required");
        }

        UUID inventoryId = inventoryRepository.findByBuffetAndProduct(buffetId, productId)
                .orElseThrow(() -> new InventoryNotFoundException(buffetId, productId))
                .getId();

        return stockMovementRepository.findByInventoryId(inventoryId);
    }
}
