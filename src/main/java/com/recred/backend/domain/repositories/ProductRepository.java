package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.Producto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
    Optional<Producto> findById(UUID id);
    ProductChangeRecommendation findBuffetLeastSoldProducts(UUID buffetId);
    List<Producto> findBestSoldProducts();
    Producto save(Producto producto);
}
