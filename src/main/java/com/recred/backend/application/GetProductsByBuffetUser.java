package com.recred.backend.application;

import com.recred.backend.domain.models.Producto;
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

    public List<Producto> execute(UUID userId) {
        if (userId != null) {
            return productRepository.findAllActiveWithStock(
                    kiosqueroRepository.findBuffetByUsuarioId(userId));
        }
        return productRepository.findAllActive();
    }
}
