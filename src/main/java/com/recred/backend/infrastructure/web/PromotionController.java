package com.recred.backend.infrastructure.web;

import com.recred.backend.application.CreatePromotion;
import com.recred.backend.application.CreatePromotionRequest;
import com.recred.backend.application.CreatePromotionResponse;
import com.recred.backend.application.DeletePromotion;
import com.recred.backend.application.DeletePromotionRequest;
import com.recred.backend.application.GetActivePromotions;
import com.recred.backend.application.GetActivePromotionsRequest;
import com.recred.backend.application.GetPromotionById;
import com.recred.backend.application.GetPromotionByIdRequest;
import com.recred.backend.application.UpdatePromotion;
import com.recred.backend.application.UpdatePromotionRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/promotions")
public class PromotionController {

    private final CreatePromotion createPromotion;
    private final UpdatePromotion updatePromotion;
    private final DeletePromotion deletePromotion;
    private final GetActivePromotions getActivePromotions;
    private final com.recred.backend.application.GetAllPromotionsByBuffet getAllPromotionsByBuffet;
    private final GetPromotionById getPromotionById;
    private final com.recred.backend.application.TogglePromotionStatus togglePromotionStatus;
    private final PlanAccessGuard planAccessGuard;

    public PromotionController(
            CreatePromotion createPromotion,
            UpdatePromotion updatePromotion,
            DeletePromotion deletePromotion,
            GetActivePromotions getActivePromotions,
            com.recred.backend.application.GetAllPromotionsByBuffet getAllPromotionsByBuffet,
            GetPromotionById getPromotionById,
            com.recred.backend.application.TogglePromotionStatus togglePromotionStatus,
            PlanAccessGuard planAccessGuard) {
        this.createPromotion = createPromotion;
        this.updatePromotion = updatePromotion;
        this.deletePromotion = deletePromotion;
        this.getActivePromotions = getActivePromotions;
        this.getAllPromotionsByBuffet = getAllPromotionsByBuffet;
        this.getPromotionById = getPromotionById;
        this.togglePromotionStatus = togglePromotionStatus;
        this.planAccessGuard = planAccessGuard;
    }

    @GetMapping
    public ResponseEntity<List<PromotionResponseDTO>> listActivePromotions() {
        List<PromotionResponseDTO> response = getActivePromotions
                .execute(new GetActivePromotionsRequest())
                .promotions()
                .stream()
                .map(PromotionResponseDTO::fromApplication)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/buffet/{buffetId}")
    public ResponseEntity<List<PromotionResponseDTO>> listActivePromotionsByBuffet(
            @PathVariable UUID buffetId) {
        List<PromotionResponseDTO> response = getAllPromotionsByBuffet
                .execute(new com.recred.backend.application
                        .GetActivePromotionsByBuffetRequest(buffetId))
                .promotions()
                .stream()
                .map(PromotionResponseDTO::fromApplication)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromotionResponseDTO> getPromotionById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(PromotionResponseDTO.fromApplication(
                getPromotionById.execute(new GetPromotionByIdRequest(id))));
    }

    @PostMapping
    public ResponseEntity<PromotionResponseDTO> createPromotion(
            Authentication authentication,
            @Valid @RequestBody PromotionRequestDTO request) {
        planAccessGuard.requireVendorAdvanced(authentication);
        CreatePromotionResponse response = createPromotion.execute(
                new CreatePromotionRequest(
                        request.getProductIds(),
                        request.getDiscountPercentage(),
                        request.getName(),
                        request.getStartDate(),
                        request.getEndDate(),
                        request.getBuffetId(),
                        false,
                        request.getImageUrl()
                )
        );
        return ResponseEntity.ok(PromotionResponseDTO.fromApplication(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromotionResponseDTO> updatePromotion(
            @PathVariable UUID id,
            Authentication authentication,
            @Valid @RequestBody PromotionRequestDTO request) {
        planAccessGuard.requireVendorAdvanced(authentication);
        CreatePromotionResponse response = updatePromotion.execute(new UpdatePromotionRequest(
                id,
                request.getProductIds(),
                request.getDiscountPercentage(),
                request.getName(),
                request.getStartDate(),
                request.getEndDate()
        ));
        return ResponseEntity.ok(PromotionResponseDTO.fromApplication(response));
    }

    @org.springframework.web.bind.annotation.PatchMapping("/{id}/toggle-status")
    public ResponseEntity<PromotionResponseDTO> togglePromotionStatus(
            @PathVariable UUID id,
            Authentication authentication) {
        planAccessGuard.requireVendorAdvanced(authentication);
        CreatePromotionResponse response = togglePromotionStatus.execute(
                new com.recred.backend.application.TogglePromotionStatusRequest(id));
        return ResponseEntity.ok(PromotionResponseDTO.fromApplication(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromotion(
            Authentication authentication,
            @PathVariable UUID id) {
        planAccessGuard.requireVendorAdvanced(authentication);
        deletePromotion.execute(new DeletePromotionRequest(id));
        return ResponseEntity.noContent().build();
    }
}
