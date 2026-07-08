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

    public GetStockMovementsResponse execute(GetStockMovementsRequest movementsRequest) {
        UUID inventoryId = findInventoryId(movementsRequest);

        return GetStockMovementsResponse.fromDomain(stockMovementsFor(inventoryId));
    }

    private UUID findInventoryId(GetStockMovementsRequest movementsRequest) {
        return inventoryRepository.findByBuffetAndProduct(
                        movementsRequest.buffetId(),
                        movementsRequest.productId())
                .orElseThrow(() -> new InventoryNotFoundException(
                        movementsRequest.buffetId(),
                        movementsRequest.productId()))
                .getId();
    }

    private List<MovimientoStock> stockMovementsFor(UUID inventoryId) {
        return stockMovementRepository.findByInventoryId(inventoryId);
    }
}
