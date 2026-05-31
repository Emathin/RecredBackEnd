package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Product;
import com.recred.backend.domain.models.ProductChangeRecommendation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
    Optional<Product> findById(UUID id);
    Product save(Product product);
    void updatePrice(UUID productId, BigDecimal newPrice);
    ProductChangeRecommendation findBuffetLeastSoldProducts(UUID buffetId);
    List<Product> findBestSoldProducts();

}
