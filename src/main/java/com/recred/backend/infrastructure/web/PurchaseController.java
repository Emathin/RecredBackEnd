package com.recred.backend.infrastructure.web;

import com.recred.backend.application.ProcessPresentialPurchase;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final ProcessPresentialPurchase processPresentialPurchase;
    private final ProductRepository productRepository;

    @PostMapping("/presential")
    public ResponseEntity<Void> processPurchase(@RequestBody ProcessPurchaseRequestDTO request) {
        Map<Producto, Integer> cart = request.getItems().stream()
                .collect(Collectors.toMap(
                        item -> productRepository.findById(item.getProductId())
                                .orElseThrow(() -> new RuntimeException("Product not found: " + item.getProductId())),
                        PurchaseItemDTO::getQuantity
                ));

        processPresentialPurchase.execute(request.getStudentId(), cart);
        return ResponseEntity.ok().build();
    }
}
