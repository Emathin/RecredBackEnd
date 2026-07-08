package com.recred.backend.application;

import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.ProductRepository;
import java.util.List;

public class ListInventoryProducts {

    private final ProductRepository productRepository;

    public ListInventoryProducts(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ListInventoryProductsResponse execute(ListInventoryProductsRequest request) {
        validate(request);
        return ListInventoryProductsResponse.fromDomain(inventoryProducts(request));
    }

    private void validate(ListInventoryProductsRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }

        if (request.buffetId() == null) {
            throw new IllegalArgumentException("buffetId is required");
        }
    }

    private List<Producto> inventoryProducts(ListInventoryProductsRequest request) {
        return productRepository.findAllActiveWithStock(request.buffetId()).stream()
                .filter(Producto::hasInventoryData)
                .toList();
    }
}
