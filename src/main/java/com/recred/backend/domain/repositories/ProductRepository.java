package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Producto;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
    Optional<Producto> findById(UUID id);
    Producto save(Producto producto);
}
