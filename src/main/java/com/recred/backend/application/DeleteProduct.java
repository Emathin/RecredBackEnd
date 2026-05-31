package com.recred.backend.application;

import com.recred.backend.domain.exceptions.ProductNotFoundException;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.ProductRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class DeleteProduct {

    private final ProductRepository productRepository;

    public DeleteProduct(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public void execute(UUID id) {
        Producto product = productRepository.findActiveById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.deactivate();
        productRepository.save(product);
    }
}
