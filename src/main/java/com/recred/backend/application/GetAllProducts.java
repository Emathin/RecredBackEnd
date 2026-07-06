package com.recred.backend.application;

import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.ProductRepository;

import java.util.List;

public class GetAllProducts {

    private final ProductRepository productRepository;

    public GetAllProducts(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public GetAllProductsResponse execute(GetAllProductsRequest request) {
        return new GetAllProductsResponse(productsFor(request).stream()
                .map(ProductResponse::fromDomain)
                .toList());
    }

    private List<Producto> productsFor(GetAllProductsRequest request) {
        if (request.buffetId() != null) {
            return productRepository.findAllActiveWithStock(request.buffetId());
        }

        return productRepository.findAllActive();
    }
}
