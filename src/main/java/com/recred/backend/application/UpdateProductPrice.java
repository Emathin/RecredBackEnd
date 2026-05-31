package com.recred.backend.application;

import com.recred.backend.domain.events.ProductPriceChangedEvent;
import com.recred.backend.domain.events.ProductPriceChangedPublisher;
import com.recred.backend.domain.models.Product;
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
    public void execute(UUID productId, BigDecimal newPrice, UUID buffetId) {
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Product price must be greater than zero");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        BigDecimal oldPrice = product.getPrice();

        if (oldPrice.compareTo(newPrice) != 0) {
            product.setPrice(newPrice);
            productRepository.updatePrice(productId, newPrice);

            eventPublisher.publish(new ProductPriceChangedEvent(
                    product,
                    oldPrice,
                    newPrice,
                    buffetId
            ));
        }
    }
}
