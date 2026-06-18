package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.models.Producto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository {
    Inventario save(Inventario inventario);
    Optional<Inventario> findByBuffetAndProduct(UUID buffetId, UUID productoId);
    Optional<Inventario> findLockedByBuffetAndProduct(UUID buffetId, UUID productoId);
    boolean checkIfProductIsInStock(UUID productId, UUID buffetId);
    List<Producto> findAvailableProductsByBuffet(UUID buffetId);
    void updateUrlImagen(UUID buffetId, UUID productoId, String urlImagen);
}
