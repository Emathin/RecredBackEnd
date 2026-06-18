package com.recred.backend.infrastructure.web;

import com.recred.backend.application.CreatePromotion;
import com.recred.backend.application.DeletePromotion;
import com.recred.backend.application.GetActivePromotions;
import com.recred.backend.application.GetPromotionById;
import com.recred.backend.application.UpdatePromotion;
import com.recred.backend.domain.models.Promotion;
import org.springframework.http.ResponseEntity;
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
    private final GetPromotionById getPromotionById;

    public PromotionController(
            CreatePromotion createPromotion,
            UpdatePromotion updatePromotion,
            DeletePromotion deletePromotion,
            GetActivePromotions getActivePromotions,
            GetPromotionById getPromotionById) {
        this.createPromotion = createPromotion;
        this.updatePromotion = updatePromotion;
        this.deletePromotion = deletePromotion;
        this.getActivePromotions = getActivePromotions;
        this.getPromotionById = getPromotionById;
    }

    @GetMapping
    public ResponseEntity<List<PromotionResponseDTO>> listActivePromotions() {
        List<PromotionResponseDTO> response = getActivePromotions.execute().stream()
                .map(PromotionResponseDTO::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromotionResponseDTO> getPromotionById(@PathVariable UUID id) {
        Promotion promotion = getPromotionById.execute(id);
        return ResponseEntity.ok(PromotionResponseDTO.fromDomain(promotion));
    }

    @PostMapping
    public ResponseEntity<PromotionResponseDTO> createPromotion(
            @RequestBody PromotionRequestDTO request) {
        Promotion promotion = createPromotion.execute(
                request.getProductIds(),
                request.getDiscountPercentage(),
                request.getName(),
                request.getStartDate(),
                request.getEndDate(),
                request.getBuffetId(),
                false
        );
        return ResponseEntity.ok(PromotionResponseDTO.fromDomain(promotion));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromotionResponseDTO> updatePromotion(
            @PathVariable UUID id, 
            @RequestBody PromotionRequestDTO request) {
        Promotion promotion = updatePromotion.execute(
                id,
                request.getProductIds(),
                request.getDiscountPercentage(),
                request.getName(),
                request.getStartDate(),
                request.getEndDate()
        );
        return ResponseEntity.ok(PromotionResponseDTO.fromDomain(promotion));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromotion(@PathVariable UUID id) {
        deletePromotion.execute(id);
        return ResponseEntity.noContent().build();
    }
}
