package com.recred.backend.infrastructure.web;

import com.recred.backend.application.CancelPresentialPurchase;
import com.recred.backend.application.CreateAdvancePurchase;
import com.recred.backend.application.GetPresentialPurchase;
import com.recred.backend.application.GetPurchaseStatus;
import com.recred.backend.application.ProcessPresentialPurchase;
import com.recred.backend.application.UpdatePurchaseStatus;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/purchases")
public class PurchaseController {

    private final ProcessPresentialPurchase processPresentialPurchase;
    private final GetPresentialPurchase getPresentialPurchase;
    private final GetPurchaseStatus getPurchaseStatus;
    private final UpdatePurchaseStatus updatePurchaseStatus;
    private final CancelPresentialPurchase cancelPresentialPurchase;
    private final ProductRepository productRepository;
    private final CreateAdvancePurchase createAdvancePurchase;

    public PurchaseController(ProcessPresentialPurchase processPresentialPurchase,
                              GetPresentialPurchase getPresentialPurchase,
                              GetPurchaseStatus getPurchaseStatus,
                              UpdatePurchaseStatus updatePurchaseStatus,
                              CancelPresentialPurchase cancelPresentialPurchase,
                              ProductRepository productRepository,
                              CreateAdvancePurchase createAdvancePurchase) {
        this.processPresentialPurchase = processPresentialPurchase;
        this.getPresentialPurchase = getPresentialPurchase;
        this.getPurchaseStatus = getPurchaseStatus;
        this.updatePurchaseStatus = updatePurchaseStatus;
        this.cancelPresentialPurchase = cancelPresentialPurchase;
        this.productRepository = productRepository;
        this.createAdvancePurchase = createAdvancePurchase;
    }

    @PostMapping("/presential")
    public ResponseEntity<PurchaseResponseDTO> processPurchase(
            @RequestBody ProcessPurchaseRequestDTO request,
            Authentication authentication
    ) {
        Map<Producto, Integer> cart = request.getItems().stream()
                .collect(Collectors.toMap(
                        item -> productRepository.findById(item.getProductId())
                                .orElseThrow(() ->
                                new RuntimeException("Product not found: " + item.getProductId())),
                        PurchaseItemDTO::getQuantity
                ));

        com.recred.backend.domain.models.MedioPago medioPago = com.recred.backend.domain.models.MedioPago.CREDITOS;
        if (request.getPaymentMethod() != null) {
            medioPago = com.recred.backend.domain.models.MedioPago.valueOf(request.getPaymentMethod());
        }

        Purchase purchase = processPresentialPurchase.execute(
                request.getStudentId(), cart, cognitoSubject(authentication), medioPago);
        return ResponseEntity.ok(new PurchaseResponseDTO(purchase));
    }

    @PostMapping("/advance")
    public ResponseEntity<AdvancePurchaseResponse> createAdvancePurchase(
            @RequestBody AdvancePurchaseRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                createAdvancePurchase.execute(request, cognitoSubject(authentication))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseResponseDTO> getPurchaseById(@PathVariable UUID id) {
        return getPresentialPurchase.execute(id)
                .map(PurchaseResponseDTO::new)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<PurchaseStatusResponseDTO> getPurchaseStatusById(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(PurchaseStatusResponseDTO.fromDomain(
                getPurchaseStatus.execute(cognitoSubject(authentication), id)
        ));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PurchaseStatusResponseDTO> updatePurchaseStatus(
            @PathVariable UUID id,
            @RequestBody UpdatePurchaseStatusRequestDTO request
    ) {
        return ResponseEntity.ok(PurchaseStatusResponseDTO.fromDomain(
                updatePurchaseStatus.execute(id, request.status())
        ));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelPurchase(@PathVariable UUID id) {
        cancelPresentialPurchase.execute(id);
        return ResponseEntity.ok().build();
    }

    private String cognitoSubject(Authentication authentication) {
        return Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario no autenticado"));
    }
}
