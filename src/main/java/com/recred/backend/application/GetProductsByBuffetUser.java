package com.recred.backend.application;

import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.ProductRepository;

import java.util.List;
import java.util.UUID;

public class GetProductsByBuffetUser {

    private final ProductRepository productRepository;
    private final KiosqueroRepository kiosqueroRepository;

    public GetProductsByBuffetUser(ProductRepository productRepository
            , KiosqueroRepository kiosqueroRepository) {
        this.productRepository = productRepository;
        this.kiosqueroRepository = kiosqueroRepository;
    }

    public List<ProductResponse> execute(UUID userId) {
        if (userId != null) {
            return productRepository.findAllActiveWithStock(
                    kiosqueroRepository.findBuffetByUsuarioId(userId))
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
