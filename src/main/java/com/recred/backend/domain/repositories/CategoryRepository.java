package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.CategoriaProducto;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {
    Optional<CategoriaProducto> findById(UUID id);
    Optional<CategoriaProducto> findByDescription(String description);
    CategoriaProducto save(CategoriaProducto category);
}
