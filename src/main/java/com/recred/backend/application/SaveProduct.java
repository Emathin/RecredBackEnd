package com.recred.backend.application;

import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.ProductRepository;

public class SaveProduct {

    private final ProductRepository productRepository;

    public SaveProduct(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Producto save(Producto productoModel) {

        return productRepository.save(productoModel);
    }


}
