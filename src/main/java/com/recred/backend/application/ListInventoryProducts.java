package com.recred.backend.application;

import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.ProductRepository;
import java.util.List;
import java.util.UUID;

public class ListInventoryProducts {

    private final ProductRepository productRepository;

    public ListInventoryProducts(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Producto> execute(UUID buffetId) {
        if (buffetId == null) {
            throw new IllegalArgumentException("buffetId is required");
        }

        return productRepository.findAllActiveWithStock(buffetId).stream()
                .filter(this::hasInventory)
                .toList();
    }

    private boolean hasInventory(Producto product) {
        return product.getStockActual() != null
                || product.getStockReservado() != null
                || product.getStockMinimo() != null
                || product.getEstadoInventario() != null
                || product.getTipoManejoInventario() != null;
    }
}
