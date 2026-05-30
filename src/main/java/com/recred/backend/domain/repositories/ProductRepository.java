package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.Producto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
    Optional<Producto> findById(UUID id);
    List<Producto> findAllActive();
    Optional<Producto> findActiveById(UUID id);
    List<Producto> findAllActiveWithStock(UUID buffetId);
    Optional<Producto> findActiveByIdWithStock(UUID id, UUID buffetId);
    Producto save(Producto producto);
    ProductChangeRecommendation findBuffetLeastSoldProducts(UUID buffetId);
    List<Producto> findBestSoldProducts();
}
