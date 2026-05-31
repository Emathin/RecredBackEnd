package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.ProductCategory;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {
    Optional<ProductCategory> findById(UUID id);
    Optional<ProductCategory> findByDescription(String description);
    ProductCategory save(ProductCategory category);
}
