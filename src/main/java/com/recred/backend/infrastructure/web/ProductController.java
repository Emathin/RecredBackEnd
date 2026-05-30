package com.recred.backend.infrastructure.web;

import com.recred.backend.application.UpdateProductPrice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
            @RequestParam BigDecimal nuevoPrecio,
            @RequestParam(required = false) UUID buffetId
    ) {
        updateProductPrice.execute(productId, nuevoPrecio, buffetId);
        return ResponseEntity.ok().build();
    }
}
