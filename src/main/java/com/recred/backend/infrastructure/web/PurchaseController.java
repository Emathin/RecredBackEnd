package com.recred.backend.infrastructure.web;

import com.recred.backend.application.CancelPresentialPurchase;
import com.recred.backend.application.GetPresentialPurchase;
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
    private final GetPresentialPurchase getPresentialPurchase;
    private final CancelPresentialPurchase cancelPresentialPurchase;
    private final ProductRepository productRepository;

    public PurchaseController(ProcessPresentialPurchase processPresentialPurchase,
                              GetPresentialPurchase getPresentialPurchase,
                              CancelPresentialPurchase cancelPresentialPurchase,
                              ProductRepository productRepository) {
        this.processPresentialPurchase = processPresentialPurchase;
        this.getPresentialPurchase = getPresentialPurchase;
        this.cancelPresentialPurchase = cancelPresentialPurchase;
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
        return getPresentialPurchase.execute(id)
                .map(purchase -> new PurchaseResponseDTO(purchase))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelPurchase(@PathVariable UUID id) {
        cancelPresentialPurchase.execute(id);
        return ResponseEntity.ok().build();
    }
}
