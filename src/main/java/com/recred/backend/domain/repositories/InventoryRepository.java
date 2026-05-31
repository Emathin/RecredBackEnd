package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Inventory;
import com.recred.backend.domain.models.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository {
    Inventory save(Inventory inventory);
    Optional<Inventory> findByBuffetAndProduct(UUID buffetId, UUID productoId);
    boolean checkIfProductIsInStock(UUID productId, UUID buffetId);
    List<Product> findAvailableProductsByBuffet(UUID buffetId);
}
