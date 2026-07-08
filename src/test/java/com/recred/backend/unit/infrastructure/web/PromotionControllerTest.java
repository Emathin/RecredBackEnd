package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.infrastructure.web.*;

import com.recred.backend.application.CreatePromotion;
import com.recred.backend.application.CreatePromotionRequest;
import com.recred.backend.application.CreatePromotionResponse;
import com.recred.backend.application.DeletePromotion;
import com.recred.backend.application.DeletePromotionRequest;
import com.recred.backend.application.GetActivePromotions;
import com.recred.backend.application.GetActivePromotionsRequest;
import com.recred.backend.application.GetActivePromotionsResponse;
import com.recred.backend.application.GetActivePromotionsByBuffet;
import com.recred.backend.application.GetActivePromotionsByBuffetRequest;
import com.recred.backend.application.GetActivePromotionsByBuffetResponse;
import com.recred.backend.application.GetPromotionById;
import com.recred.backend.application.GetPromotionByIdRequest;
import com.recred.backend.application.UpdatePromotion;
import com.recred.backend.application.UpdatePromotionRequest;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
    private PlanAccessGuard planAccessGuard;
    private PromotionController controller;

    @BeforeEach
    void setUp() {
        createPromotion = mock(CreatePromotion.class);
        updatePromotion = mock(UpdatePromotion.class);
        deletePromotion = mock(DeletePromotion.class);
        getActivePromotions = mock(GetActivePromotions.class);
        getActivePromotionsByBuffet = mock(GetActivePromotionsByBuffet.class);
        getPromotionById = mock(GetPromotionById.class);
        planAccessGuard = mock(PlanAccessGuard.class);
        controller = new PromotionController(
                createPromotion,
                updatePromotion,
                deletePromotion,
                getActivePromotions,
                getActivePromotionsByBuffet,
                getPromotionById,
                planAccessGuard
        );
    }

    @Test
    void listActivePromotionsReturnsMappedPromotions() {
        UUID promotionId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Promotion promotion = promotion(promotionId, productId);
        when(getActivePromotions.execute(new GetActivePromotionsRequest()))
                .thenReturn(new GetActivePromotionsResponse(
                        List.of(CreatePromotionResponse.fromDomain(promotion))));

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
        when(getActivePromotionsByBuffet.execute(
                new GetActivePromotionsByBuffetRequest(buffetId)))
                .thenReturn(new GetActivePromotionsByBuffetResponse(
                        List.of(CreatePromotionResponse.fromDomain(promotion))));

        ResponseEntity<List<PromotionResponseDTO>> response =
                controller.listActivePromotionsByBuffet(buffetId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(promotionId, response.getBody().get(0).getId());
        verify(getActivePromotionsByBuffet)
                .execute(new GetActivePromotionsByBuffetRequest(buffetId));
    }

    @Test
    void getPromotionByIdReturnsMappedPromotion() {
        UUID promotionId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(getPromotionById.execute(new GetPromotionByIdRequest(promotionId)))
                .thenReturn(CreatePromotionResponse.fromDomain(
                        promotion(promotionId, productId)));

        ResponseEntity<PromotionResponseDTO> response =
                controller.getPromotionById(promotionId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("ACTIVE", response.getBody().getStatus());
        verify(getPromotionById).execute(new GetPromotionByIdRequest(promotionId));
    }

    @Test
    void createPromotionDelegatesRequestAndReturnsResponse() {
        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        PromotionRequestDTO request = request(List.of(firstProductId, secondProductId));
        request.setBuffetId(buffetId);
        Promotion promotion = promotion(UUID.randomUUID(), firstProductId);
        CreatePromotionResponse createResponse = CreatePromotionResponse.fromDomain(promotion);
        when(createPromotion.execute(
                any(CreatePromotionRequest.class)
        )).thenReturn(createResponse);

        ResponseEntity<PromotionResponseDTO> response =
                controller.createPromotion(null, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(promotion.getId(), response.getBody().getId());
        verify(planAccessGuard).requireVendorAdvanced(null);
        verify(createPromotion).execute(
                any(CreatePromotionRequest.class)
        );
    }

    @Test
    void updatePromotionDelegatesRequestAndReturnsResponse() {
        UUID promotionId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        PromotionRequestDTO request = request(List.of(productId, UUID.randomUUID()));
        Promotion promotion = promotion(promotionId, productId);
        UpdatePromotionRequest useCaseRequest = new UpdatePromotionRequest(
                promotionId,
                request.getProductIds(),
                request.getDiscountPercentage(),
                request.getName(),
                request.getStartDate(),
                request.getEndDate()
        );
        when(updatePromotion.execute(useCaseRequest))
                .thenReturn(CreatePromotionResponse.fromDomain(promotion));

        ResponseEntity<PromotionResponseDTO> response =
                controller.updatePromotion(promotionId, null, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(promotionId, response.getBody().getId());
        verify(planAccessGuard).requireVendorAdvanced(null);
        verify(updatePromotion).execute(useCaseRequest);
    }

    @Test
    void deletePromotionDelegatesAndReturnsNoContent() {
        UUID promotionId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.deletePromotion(null, promotionId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(planAccessGuard).requireVendorAdvanced(null);
        verify(deletePromotion).execute(new DeletePromotionRequest(promotionId));
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
