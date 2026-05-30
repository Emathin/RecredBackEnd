package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.Producto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
    Optional<Producto> findById(UUID id);
    Producto save(Producto producto);
    void updatePrice(UUID productId, BigDecimal newPrice);
    ProductChangeRecommendation findBuffetLeastSoldProducts(UUID buffetId);
    List<Producto> findBestSoldProducts();

}
