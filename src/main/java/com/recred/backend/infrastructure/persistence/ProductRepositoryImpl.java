package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    public ProductRepositoryImpl(ProductJpaRepository productJpaRepository) {
        this.productJpaRepository = productJpaRepository;
    }

    @Override
    public ProductChangeRecommendation findBuffetLeastSoldProducts(UUID buffetId) {
         ProductoEntity producto = productJpaRepository.findBuffetLeastSoldProducts(buffetId);

        return producto.toProductRecommendation(buffetId);
    }

}
