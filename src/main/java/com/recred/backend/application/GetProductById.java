package com.recred.backend.application;

import com.recred.backend.domain.exceptions.ProductNotFoundException;
import com.recred.backend.domain.repositories.ProductRepository;

import java.util.UUID;

public class GetProductById {

    private final ProductRepository productRepository;

    public GetProductById(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse execute(UUID id, UUID buffetId) {
        if (buffetId != null) {
            return productRepository.findActiveByIdWithStock(id, buffetId)
                    .map(ProductResponse::fromDomain)
                    .orElseThrow(() -> new ProductNotFoundException(id));
        }
        return productRepository.findActiveById(id)
                .map(ProductResponse::fromDomain)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
