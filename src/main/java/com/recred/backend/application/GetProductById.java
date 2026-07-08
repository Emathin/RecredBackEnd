package com.recred.backend.application;

import com.recred.backend.domain.exceptions.ProductNotFoundException;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.ProductRepository;

public class GetProductById {

    private final ProductRepository productRepository;

    public GetProductById(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse execute(GetProductByIdRequest request) {
        return ProductResponse.fromDomain(productFor(request));
    }

    private Producto productFor(GetProductByIdRequest request) {
        if (request.buffetId() != null) {
            return productRepository.findActiveByIdWithStock(request.id(), request.buffetId())
                    .orElseThrow(() -> new ProductNotFoundException(request.id()));
        }

        return productRepository.findActiveById(request.id())
                .orElseThrow(() -> new ProductNotFoundException(request.id()));
    }
}
