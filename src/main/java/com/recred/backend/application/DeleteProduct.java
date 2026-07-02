package com.recred.backend.application;

import com.recred.backend.domain.exceptions.ProductNotFoundException;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.ProductRepository;
import org.springframework.transaction.annotation.Transactional;

public class DeleteProduct {

    private final ProductRepository productRepository;

    public DeleteProduct(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public DeleteProductResponse execute(DeleteProductRequest request) {
        Producto product = productRepository.findActiveById(request.id())
                .orElseThrow(() -> new ProductNotFoundException(request.id()));

        product.deactivate();
        productRepository.save(product);
        return new DeleteProductResponse(product.getId());
    }
}
