package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Inventario;

import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository {
    Inventario save(Inventario inventario);
    Optional<Inventario> findByBuffetAndProduct(UUID buffetId, UUID productoId);
    boolean checkIfProductIsInStock(UUID productId, UUID buffetId);
}
