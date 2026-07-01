package com.recred.backend.application;

import com.recred.backend.domain.events.ProductPriceChangedEvent;
import com.recred.backend.domain.events.ProductPriceChangedPublisher;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.ProductRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

public class UpdateProductPrice {

    private final ProductRepository productRepository;
    private final ProductPriceChangedPublisher eventPublisher;

    public UpdateProductPrice(ProductRepository productRepository,
                              ProductPriceChangedPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(UpdateProductPriceRequest request) {
        request.validate();

        Producto product = findProduct(request.productId());
        BigDecimal oldPrice = product.getPrecio();

        if (priceChanged(oldPrice, request.newPrice())) {
            updatePrice(product, request);
            publishPriceChanged(product, oldPrice, request);
        }
    }

    private Producto findProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException(
                        "Product not found: " + productId));
    }

    private boolean priceChanged(BigDecimal oldPrice, BigDecimal newPrice) {
        return oldPrice.compareTo(newPrice) != 0;
    }

    private void updatePrice(Producto product, UpdateProductPriceRequest request) {
        product.setPrecio(request.newPrice());
        productRepository.updatePrice(request.productId(), request.newPrice());
    }

    private void publishPriceChanged(
            Producto product,
            BigDecimal oldPrice,
            UpdateProductPriceRequest request) {
        eventPublisher.publish(new ProductPriceChangedEvent(
                product,
                oldPrice,
                request.newPrice(),
                request.buffetId()
        ));
    }
}
