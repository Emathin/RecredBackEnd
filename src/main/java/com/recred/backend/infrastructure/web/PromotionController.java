package com.recred.backend.infrastructure.web;

import com.recred.backend.application.CreatePromotion;
import com.recred.backend.application.CreatePromotionRequest;
import com.recred.backend.application.CreatePromotionResponse;
import com.recred.backend.application.DeletePromotion;
import com.recred.backend.application.GetActivePromotions;
import com.recred.backend.application.GetActivePromotionsByBuffet;
import com.recred.backend.application.GetPromotionById;
import com.recred.backend.application.UpdatePromotion;
import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Producto;
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
    private final GetActivePromotionsByBuffet getActivePromotionsByBuffet;
    private final GetPromotionById getPromotionById;

    public PromotionController(
            CreatePromotion createPromotion,
            UpdatePromotion updatePromotion,
            DeletePromotion deletePromotion,
            GetActivePromotions getActivePromotions,
            GetActivePromotionsByBuffet getActivePromotionsByBuffet,
            GetPromotionById getPromotionById) {
        this.createPromotion = createPromotion;
        this.updatePromotion = updatePromotion;
        this.deletePromotion = deletePromotion;
        this.getActivePromotions = getActivePromotions;
        this.getActivePromotionsByBuffet = getActivePromotionsByBuffet;
        this.getPromotionById = getPromotionById;
    }

    @GetMapping
    public ResponseEntity<List<PromotionResponseDTO>> listActivePromotions() {
        List<PromotionResponseDTO> response = getActivePromotions.execute().stream()
                .map(PromotionResponseDTO::fromAppResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/buffet/{buffetId}")
    public ResponseEntity<List<PromotionResponseDTO>> listActivePromotionsByBuffet(
            @PathVariable UUID buffetId) {
        List<PromotionResponseDTO> response = getActivePromotionsByBuffet.execute(buffetId).stream()
                .map(PromotionResponseDTO::fromAppResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromotionResponseDTO> getPromotionById(@PathVariable UUID id) {
        com.recred.backend.application.PromotionResponse promotion = getPromotionById.execute(id);
        return ResponseEntity.ok(PromotionResponseDTO.fromAppResponse(promotion));
    }

    @PostMapping
    public ResponseEntity<PromotionResponseDTO> createPromotion(
            @RequestBody PromotionRequestDTO request) {
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
        Promotion promotion = new Promotion();
        promotion.setId(response.id());
        promotion.setName(response.name());
        promotion.setDiscountPercentage(response.discountPercentage());
        promotion.setStartDate(response.startDate());
        promotion.setEndDate(response.endDate());
        promotion.setStatus(response.status());
        promotion.setImageUrl(response.imageUrl());
        
        if (response.buffetId() != null) {
            Buffet buffet = new Buffet();
            buffet.setId(response.buffetId());
            promotion.setBuffet(buffet);
        }
        
        if (response.productIds() != null && !response.productIds().isEmpty()) {
            List<Producto> products = response.productIds().stream().map(id -> {
                Producto p = new Producto();
                p.setId(id);
                return p;
            }).collect(Collectors.toList());
            promotion.setProducts(products);
        }
        
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
