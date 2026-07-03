package com.recred.backend.infrastructure.web;

import com.recred.backend.application.CreatePromotion;
import com.recred.backend.application.CreatePromotionRequest;
import com.recred.backend.application.CreatePromotionResponse;
import com.recred.backend.application.DeletePromotion;
import com.recred.backend.application.DeletePromotionRequest;
import com.recred.backend.application.GetActivePromotions;
import com.recred.backend.application.GetActivePromotionsRequest;
import com.recred.backend.application.GetActivePromotionsByBuffet;
import com.recred.backend.application.GetActivePromotionsByBuffetRequest;
import com.recred.backend.application.GetPromotionById;
import com.recred.backend.application.GetPromotionByIdRequest;
import com.recred.backend.application.UpdatePromotion;
import com.recred.backend.application.UpdatePromotionRequest;
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
        List<PromotionResponseDTO> response = getActivePromotionsByBuffet
                .execute(new GetActivePromotionsByBuffetRequest(buffetId))
                .promotions()
                .stream()
                .map(PromotionResponseDTO::fromApplication)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromotionResponseDTO> getPromotionById(@PathVariable UUID id) {
        return ResponseEntity.ok(PromotionResponseDTO.fromApplication(
                getPromotionById.execute(new GetPromotionByIdRequest(id))));
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
        return ResponseEntity.ok(PromotionResponseDTO.fromApplication(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromotionResponseDTO> updatePromotion(
            @PathVariable UUID id, 
            @RequestBody PromotionRequestDTO request) {
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromotion(@PathVariable UUID id) {
        deletePromotion.execute(new DeletePromotionRequest(id));
        return ResponseEntity.noContent().build();
    }
}
