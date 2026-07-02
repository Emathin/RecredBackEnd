package com.recred.backend.application;

import com.recred.backend.domain.repositories.ProductRepository;

import java.util.List;
import java.util.UUID;

public class GetAllProducts {

    private final ProductRepository productRepository;

    public GetAllProducts(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponse> execute(UUID buffetId) {
        if (buffetId != null) {
            return productRepository.findAllActiveWithStock(buffetId)
                    .stream()
                    .map(ProductResponse::fromDomain)
                    .toList();
        }
        return productRepository.findAllActive()
                .stream()
                .map(ProductResponse::fromDomain)
                .toList();
    }
}
