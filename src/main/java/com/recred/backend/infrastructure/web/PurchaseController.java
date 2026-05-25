package com.recred.backend.infrastructure.web;

import com.recred.backend.application.CancelPurchase;
import com.recred.backend.application.GetPurchase;
import com.recred.backend.application.ProcessPresentialPurchase;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/purchases")
public class PurchaseController {

    private final ProcessPresentialPurchase processPresentialPurchase;
    private final GetPurchase getPurchase;
    private final CancelPurchase cancelPurchase;
    private final ProductRepository productRepository;

    public PurchaseController(ProcessPresentialPurchase processPresentialPurchase,
                              GetPurchase getPurchase,
                              CancelPurchase cancelPurchase,
                              ProductRepository productRepository) {
        this.processPresentialPurchase = processPresentialPurchase;
        this.getPurchase = getPurchase;
        this.cancelPurchase = cancelPurchase;
        this.productRepository = productRepository;
    }

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

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseResponseDTO> getPurchaseById(@PathVariable UUID id) {
        return getPurchase.execute(id)
                .map(purchase -> new PurchaseResponseDTO(purchase))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelPurchase(@PathVariable UUID id) {
        cancelPurchase.execute(id);
        return ResponseEntity.ok().build();
    }
}
