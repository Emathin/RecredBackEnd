package com.recred.backend.infrastructure.web;

import com.recred.backend.application.UpdateProductPrice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/productos")
public class ProductController {

    private final UpdateProductPrice updateProductPrice;

    public ProductController(UpdateProductPrice updateProductPrice) {
        this.updateProductPrice = updateProductPrice;
    }

    @PatchMapping("/{productId}/precio")
    public ResponseEntity<Void> updatePrice(
            @PathVariable UUID productId,
            @RequestParam(required = false) BigDecimal nuevoPrecio,
            @RequestParam(required = false) UUID buffetId,
            @RequestBody(required = false) UpdateProductPriceRequest request
    ) {
        updateProductPrice.execute(
                productId,
                resolveNewPrice(nuevoPrecio, request),
                resolveBuffetId(buffetId, request)
        );
        return ResponseEntity.ok().build();
    }

    private BigDecimal resolveNewPrice(
            BigDecimal queryPrice,
            UpdateProductPriceRequest request
    ) {
        if (queryPrice != null) {
            return queryPrice;
        }
        if (request != null) {
            return request.nuevoPrecio();
        }
        throw new IllegalArgumentException("Product price is required");
    }

    private UUID resolveBuffetId(UUID queryBuffetId, UpdateProductPriceRequest request) {
        if (queryBuffetId != null) {
            return queryBuffetId;
        }
        if (request != null) {
            return request.buffetId();
        }
        return null;
    }

    public record UpdateProductPriceRequest(BigDecimal nuevoPrecio, UUID buffetId) {
    }
}
