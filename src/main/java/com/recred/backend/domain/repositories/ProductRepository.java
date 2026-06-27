package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.Producto;

import java.math.BigDecimal;
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
    void updatePrice(UUID productId, BigDecimal newPrice);
    ProductChangeRecommendation findBuffetLeastSoldProducts(UUID buffetId);
    List<ProductChangeRecommendation> findBuffetTop5LeastSoldProducts(UUID buffetId);
    List<Producto> findBestSoldProductsNotInStock(UUID buffetId);
    List<Producto> findBuffetTop3MostSold(UUID buffetId, UUID categoriaId);
    }
