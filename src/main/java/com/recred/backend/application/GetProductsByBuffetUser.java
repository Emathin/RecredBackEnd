package com.recred.backend.application;

import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.ProductRepository;

import java.util.List;

public class GetProductsByBuffetUser {

    private final ProductRepository productRepository;
    private final KiosqueroRepository kiosqueroRepository;

    public GetProductsByBuffetUser(ProductRepository productRepository
            , KiosqueroRepository kiosqueroRepository) {
        this.productRepository = productRepository;
        this.kiosqueroRepository = kiosqueroRepository;
    }

    public GetProductsByBuffetUserResponse execute(GetProductsByBuffetUserRequest request) {
        return new GetProductsByBuffetUserResponse(productsFor(request).stream()
                .map(ProductResponse::fromDomain)
                .toList());
    }

    private List<Producto> productsFor(GetProductsByBuffetUserRequest request) {
        if (request.userId() != null) {
            return productRepository.findAllActiveWithStock(
                    kiosqueroRepository.findBuffetByUsuarioId(request.userId()));
        }

        return productRepository.findAllActive();
    }
}
