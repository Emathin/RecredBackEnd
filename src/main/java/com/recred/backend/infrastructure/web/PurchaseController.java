package com.recred.backend.infrastructure.web;

import com.recred.backend.application.CancelPresentialPurchase;
import com.recred.backend.application.CreateAdvancePurchase;
import com.recred.backend.application.CreateAdvancePurchaseRequest;
import com.recred.backend.application.CreateAdvancePurchaseResponse;
import com.recred.backend.application.GetPresentialPurchase;
import com.recred.backend.application.GetPurchaseStatus;
import com.recred.backend.application.GetPurchaseStatusRequest;
import com.recred.backend.application.ProcessPresentialPurchase;
import com.recred.backend.application.ProcessPresentialPurchaseRequest;
import com.recred.backend.application.ProcessPresentialPurchaseResponse;
import com.recred.backend.application.UpdatePurchaseStatus;
import com.recred.backend.domain.models.Purchase;
import jakarta.validation.Valid;
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

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchases")
public class PurchaseController {

    private final ProcessPresentialPurchase processPresentialPurchase;
    private final GetPresentialPurchase getPresentialPurchase;
    private final GetPurchaseStatus getPurchaseStatus;
    private final UpdatePurchaseStatus updatePurchaseStatus;
    private final CancelPresentialPurchase cancelPresentialPurchase;
    private final CreateAdvancePurchase createAdvancePurchase;
    private final KiosqueroAccessGuard kiosqueroAccessGuard;
    private final PurchaseAccessGuard purchaseAccessGuard;

    public PurchaseController(ProcessPresentialPurchase processPresentialPurchase,
                              GetPresentialPurchase getPresentialPurchase,
                              GetPurchaseStatus getPurchaseStatus,
                              UpdatePurchaseStatus updatePurchaseStatus,
                              CancelPresentialPurchase cancelPresentialPurchase,
                              CreateAdvancePurchase createAdvancePurchase,
                              KiosqueroAccessGuard kiosqueroAccessGuard,
                              PurchaseAccessGuard purchaseAccessGuard) {
        this.processPresentialPurchase = processPresentialPurchase;
        this.getPresentialPurchase = getPresentialPurchase;
        this.getPurchaseStatus = getPurchaseStatus;
        this.updatePurchaseStatus = updatePurchaseStatus;
        this.cancelPresentialPurchase = cancelPresentialPurchase;
        this.createAdvancePurchase = createAdvancePurchase;
        this.kiosqueroAccessGuard = kiosqueroAccessGuard;
        this.purchaseAccessGuard = purchaseAccessGuard;
    }

    @PostMapping("/presential")
    public ResponseEntity<ProcessPresentialPurchaseResponse> processPurchase(
            @Valid @RequestBody ProcessPurchaseRequestDTO request,
            Authentication authentication
    ) {
        kiosqueroAccessGuard.requireBuffetAccess(authentication, request.getBuffetId());

        ProcessPresentialPurchaseRequest useCaseRequest = new ProcessPresentialPurchaseRequest(
                request.getStudentId(),
                request.getItems().stream()
                        .map(item -> new ProcessPresentialPurchaseRequest.ItemRequest(
                                item.getProductId(), item.getQuantity()))
                        .toList(),
                request.getBuffetId()
        );

        ProcessPresentialPurchaseResponse response = 
                processPresentialPurchase.execute(useCaseRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/advance")
    public ResponseEntity<AdvancePurchaseResponse> createAdvancePurchase(
            @Valid @RequestBody AdvancePurchaseRequest request,
            Authentication authentication
    ) {
        CreateAdvancePurchaseRequest useCaseRequest = new CreateAdvancePurchaseRequest(
                request.orders().stream()
                        .map(o -> new CreateAdvancePurchaseRequest.OrderRequest(
                                o.studentId(),
                                o.buyerId(),
                                o.buyerType(),
                                o.buffetId(),
                                o.date(),
                                o.recessTime(),
                                o.items().stream()
                                        .map(i -> new CreateAdvancePurchaseRequest.ItemRequest(
                                                i.productId(),
                                                i.quantity()
                                        ))
                                        .toList(),
                                o.suggestionId(),
                                o.tutorId()
                        ))
                        .toList()
        );

        CreateAdvancePurchaseResponse useCaseResponse = createAdvancePurchase.execute(
                useCaseRequest.withCognitoSub(cognitoSubject(authentication)));

        return ResponseEntity.ok(new AdvancePurchaseResponse(
                useCaseResponse.orderId(),
                useCaseResponse.status(),
                useCaseResponse.total(),
                useCaseResponse.codes()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseResponseDTO> getPurchaseById(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        Purchase purchase = purchaseAccessGuard.requirePurchaseAccess(authentication, id);
        com.recred.backend.application.GetPresentialPurchaseRequest useCaseRequest = 
                new com.recred.backend.application.GetPresentialPurchaseRequest(purchase.getId());
        return ResponseEntity.ok(new PurchaseResponseDTO(
                getPresentialPurchase.execute(useCaseRequest)
        ));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<PurchaseStatusResponseDTO> getPurchaseStatusById(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(PurchaseStatusResponseDTO.fromApplication(
                getPurchaseStatus.execute(new GetPurchaseStatusRequest(
                        cognitoSubject(authentication),
                        id
                ))
        ));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PurchaseStatusResponseDTO> updatePurchaseStatus(
            @PathVariable UUID id,
            @RequestBody UpdatePurchaseStatusRequestDTO request,
            Authentication authentication
    ) {
        Purchase purchase = purchaseAccessGuard.requirePurchaseAccess(authentication, id);
        kiosqueroAccessGuard.requireBuffetAccess(authentication, purchase.getBuffetId());
        
        com.recred.backend.application.UpdatePurchaseStatusRequest useCaseRequest = 
                new com.recred.backend.application.UpdatePurchaseStatusRequest(
                        id, request.status());
        com.recred.backend.application.UpdatePurchaseStatusResponse useCaseResponse = 
                updatePurchaseStatus.execute(useCaseRequest);
                
        return ResponseEntity.ok(new PurchaseStatusResponseDTO(
                useCaseResponse.id(),
                useCaseResponse.studentId(),
                useCaseResponse.status(),
                useCaseResponse.label(),
                useCaseResponse.date()
        ));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelPurchase(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        purchaseAccessGuard.requirePurchaseAccess(authentication, id);
        cancelPresentialPurchase.execute(
                new com.recred.backend.application.CancelPresentialPurchaseRequest(id));
        return ResponseEntity.ok().build();
    }

    private String cognitoSubject(Authentication authentication) {
        return Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario no autenticado"));
    }
}
