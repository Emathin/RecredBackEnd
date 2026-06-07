package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetProductsByBuffetUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/buffet")
public class BuffetController {

    private final GetProductsByBuffetUser getProductsByBuffetUser;

    public BuffetController(GetProductsByBuffetUser getProductsByBuffetUser
                            ) {
        this.getProductsByBuffetUser = getProductsByBuffetUser;
    }

    @GetMapping("/user/{userId}/products")
    public ResponseEntity<List<ProductResponseDTO>> findAllByUserId(
            @PathVariable UUID userId) {
        List<ProductResponseDTO> products =
                getProductsByBuffetUser.execute(userId)
                .stream()
                .map(ProductResponseDTO::fromDomain)
                .toList();
        return ResponseEntity.ok(products);
    }
}
