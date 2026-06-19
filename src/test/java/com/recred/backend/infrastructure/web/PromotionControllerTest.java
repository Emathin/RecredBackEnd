package com.recred.backend.web;

import com.recred.backend.application.CreatePromotion;
import com.recred.backend.application.DeletePromotion;
import com.recred.backend.application.GetActivePromotions;
import com.recred.backend.application.GetActivePromotionsByBuffet;
import com.recred.backend.application.GetPromotionById;
import com.recred.backend.application.UpdatePromotion;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.infrastructure.web.PromotionController;
import com.recred.backend.infrastructure.web.PromotionRequestDTO;
import com.recred.backend.infrastructure.web.PromotionResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PromotionControllerTest {

    private CreatePromotion createPromotion;
    private UpdatePromotion updatePromotion;
    private DeletePromotion deletePromotion;
    private GetActivePromotions getActivePromotions;
    private GetActivePromotionsByBuffet getActivePromotionsByBuffet;
    private GetPromotionById getPromotionById;
    private PromotionController controller;

    @BeforeEach
    void setUp() {
        createPromotion = mock(CreatePromotion.class);
        updatePromotion = mock(UpdatePromotion.class);
        deletePromotion = mock(DeletePromotion.class);
        getActivePromotions = mock(GetActivePromotions.class);
        getActivePromotionsByBuffet = mock(GetActivePromotionsByBuffet.class);
        getPromotionById = mock(GetPromotionById.class);
        controller = new PromotionController(
                createPromotion,
                updatePromotion,
                deletePromotion,
                getActivePromotions,
                getActivePromotionsByBuffet,
                getPromotionById
        );
    }

    @Test
    void listActivePromotionsReturnsMappedPromotions() {
        UUID promotionId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Promotion promotion = promotion(promotionId, productId);
        when(getActivePromotions.execute()).thenReturn(List.of(promotion));

        ResponseEntity<List<PromotionResponseDTO>> response =
                controller.listActivePromotions();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(promotionId, response.getBody().get(0).getId());
        assertEquals("Combo", response.getBody().get(0).getName());
        assertEquals(List.of(productId), response.getBody().get(0).getProductIds());
    }

    @Test
    void listActivePromotionsByBuffetReturnsMappedPromotions() {
        UUID buffetId = UUID.randomUUID();
        UUID promotionId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Promotion promotion = promotion(promotionId, productId);
        when(getActivePromotionsByBuffet.execute(buffetId)).thenReturn(List.of(promotion));

        ResponseEntity<List<PromotionResponseDTO>> response =
                controller.listActivePromotionsByBuffet(buffetId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(promotionId, response.getBody().get(0).getId());
        verify(getActivePromotionsByBuffet).execute(buffetId);
    }

    @Test
    void getPromotionByIdReturnsMappedPromotion() {
        UUID promotionId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(getPromotionById.execute(promotionId))
                .thenReturn(promotion(promotionId, productId));

        ResponseEntity<PromotionResponseDTO> response =
                controller.getPromotionById(promotionId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("ACTIVE", response.getBody().getStatus());
        verify(getPromotionById).execute(promotionId);
    }

    @Test
    void createPromotionDelegatesRequestAndReturnsResponse() {
        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        PromotionRequestDTO request = request(List.of(firstProductId, secondProductId));
        request.setBuffetId(buffetId);
        Promotion promotion = promotion(UUID.randomUUID(), firstProductId);
        when(createPromotion.execute(
                request.getProductIds(),
                request.getDiscountPercentage(),
                request.getName(),
                request.getStartDate(),
                request.getEndDate(),
                request.getBuffetId(),
                false
        )).thenReturn(promotion);

        ResponseEntity<PromotionResponseDTO> response =
                controller.createPromotion(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(promotion.getId(), response.getBody().getId());
        verify(createPromotion).execute(
                request.getProductIds(),
                request.getDiscountPercentage(),
                request.getName(),
                request.getStartDate(),
                request.getEndDate(),
                request.getBuffetId(),
                false
        );
    }

    @Test
    void updatePromotionDelegatesRequestAndReturnsResponse() {
        UUID promotionId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        PromotionRequestDTO request = request(List.of(productId, UUID.randomUUID()));
        Promotion promotion = promotion(promotionId, productId);
        when(updatePromotion.execute(
                promotionId,
                request.getProductIds(),
                request.getDiscountPercentage(),
                request.getName(),
                request.getStartDate(),
                request.getEndDate()
        )).thenReturn(promotion);

        ResponseEntity<PromotionResponseDTO> response =
                controller.updatePromotion(promotionId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(promotionId, response.getBody().getId());
        verify(updatePromotion).execute(
                promotionId,
                request.getProductIds(),
                request.getDiscountPercentage(),
                request.getName(),
                request.getStartDate(),
                request.getEndDate()
        );
    }

    @Test
    void deletePromotionDelegatesAndReturnsNoContent() {
        UUID promotionId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.deletePromotion(promotionId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(deletePromotion).execute(promotionId);
    }

    private PromotionRequestDTO request(List<UUID> productIds) {
        PromotionRequestDTO request = new PromotionRequestDTO();
        request.setName("Combo");
        request.setDiscountPercentage(new BigDecimal("15.00"));
        request.setStartDate(LocalDateTime.of(2026, 6, 1, 8, 0));
        request.setEndDate(LocalDateTime.of(2026, 6, 30, 23, 0));
        request.setProductIds(productIds);
        return request;
    }

    private Promotion promotion(UUID promotionId, UUID productId) {
        Producto product = new Producto();
        product.setId(productId);

        Promotion promotion = new Promotion();
        promotion.setId(promotionId);
        promotion.setName("Combo");
        promotion.setDiscountPercentage(new BigDecimal("15.00"));
        promotion.setStartDate(LocalDateTime.of(2026, 6, 1, 8, 0));
        promotion.setEndDate(LocalDateTime.of(2026, 6, 30, 23, 0));
        promotion.setStatus(PromotionStatus.ACTIVE);
        promotion.setProducts(List.of(product));
        return promotion;
    }
}
