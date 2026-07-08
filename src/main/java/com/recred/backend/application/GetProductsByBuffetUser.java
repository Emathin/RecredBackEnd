package com.recred.backend.application;

import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.ProductRepository;

import java.util.List;
import java.util.UUID;

public class GetProductsByBuffetUser {

    private final ProductRepository productRepository;
    private final KiosqueroRepository kiosqueroRepository;

    public GetProductsByBuffetUser(
            ProductRepository productRepository,
            KiosqueroRepository kiosqueroRepository
    ) {
        this.productRepository = productRepository;
        this.kiosqueroRepository = kiosqueroRepository;
    }

    public GetProductsByBuffetUserResponse execute(
            GetProductsByBuffetUserRequest productsRequest) {
        return new GetProductsByBuffetUserResponse(productsVisibleFor(productsRequest).stream()
                .map(ProductResponse::fromDomain)
                .toList());
    }

    private List<Producto> productsVisibleFor(GetProductsByBuffetUserRequest productsRequest) {
        if (productsRequest.userId() == null) {
            return productRepository.findAllActive();
        }

        UUID buffetId = kiosqueroRepository.findBuffetByUsuarioId(productsRequest.userId());
        return productRepository.findAllActiveWithStock(buffetId);
    }
}
