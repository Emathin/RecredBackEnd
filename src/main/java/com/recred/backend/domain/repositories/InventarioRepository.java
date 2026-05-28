package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Producto;
import java.util.List;
import java.util.UUID;

public interface InventarioRepository {
    List<Producto> findAvailableProductsByBuffet(UUID buffetId);
}
