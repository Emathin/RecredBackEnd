package com.recred.backend.unit.infrastructure.web;

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
import com.recred.backend.application.GetAllPromotionsByBuffet;
import com.recred.backend.application.GetPromotionById;
import com.recred.backend.application.GetPromotionByIdRequest;
import com.recred.backend.application.TogglePromotionStatus;
import com.recred.backend.application.UpdatePromotion;
import com.recred.backend.application.UpdatePromotionRequest;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.infrastructure.web.PlanAccessGuard;
import com.recred.backend.infrastructure.web.PromotionController;
import com.recred.backend.infrastructure.web.PromotionRequestDTO;
import com.recred.backend.infrastructure.web.PromotionResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionControllerTest {

    private static final UUID PROMOTION_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID SECOND_PRODUCT_ID = UUID.randomUUID();
    private static final UUID BUFFET_ID = UUID.randomUUID();

    @Mock
    private CreatePromotion createPromotion;
    @Mock
    private UpdatePromotion updatePromotion;
    @Mock
    private DeletePromotion deletePromotion;
    @Mock
    private GetActivePromotions getActivePromotions;
    @Mock
    private GetAllPromotionsByBuffet getAllPromotionsByBuffet;
    @Mock
    private GetPromotionById getPromotionById;
    @Mock
    private TogglePromotionStatus togglePromotionStatus;
    @Mock
    private PlanAccessGuard planAccessGuard;

    private PromotionController controller;

    @BeforeEach
    void setUp() {
        controller = new PromotionController(
                createPromotion,
                updatePromotion,
                deletePromotion,
                getActivePromotions,
                getAllPromotionsByBuffet,
                getPromotionById,
                togglePromotionStatus,
                planAccessGuard
        );
    }

    @Test
    void listActivePromotionsReturnsMappedPromotions() {
        givenActivePromotionsReturned(PROMOTION_ID, PRODUCT_ID);

        ResponseEntity<List<PromotionResponseDTO>> response = controller.listActivePromotions();

        thenListActivePromotionsReturnsCorrectly(response, PROMOTION_ID, PRODUCT_ID);
    }

    @Test
    void listActivePromotionsByBuffetReturnsMappedPromotions() {
        givenActivePromotionsByBuffetReturned(BUFFET_ID, PROMOTION_ID, PRODUCT_ID);

        ResponseEntity<List<PromotionResponseDTO>> response = 
                controller.listActivePromotionsByBuffet(BUFFET_ID);

        thenListActivePromotionsByBuffetReturnsCorrectly(response, BUFFET_ID, PROMOTION_ID);
    }

    @Test
    void getPromotionByIdReturnsMappedPromotion() {
        givenPromotionByIdReturned(PROMOTION_ID, PRODUCT_ID);

        ResponseEntity<PromotionResponseDTO> response = controller.getPromotionById(PROMOTION_ID);

        thenPromotionByIdReturnsCorrectly(response, PROMOTION_ID);
    }

    @Test
    void createPromotionDelegatesRequestAndReturnsResponse() {
        PromotionRequestDTO request = givenPromotionRequest(PRODUCT_ID, SECOND_PRODUCT_ID, BUFFET_ID);
        givenPromotionCreated(PROMOTION_ID, PRODUCT_ID);

        ResponseEntity<PromotionResponseDTO> response = controller.createPromotion(null, request);

        thenPromotionIsCreatedSuccessfully(response, PROMOTION_ID);
    }

    @Test
    void updatePromotionDelegatesRequestAndReturnsResponse() {
        PromotionRequestDTO request = givenPromotionRequest(PRODUCT_ID, SECOND_PRODUCT_ID, null);
        UpdatePromotionRequest useCaseRequest = givenPromotionUpdated(PROMOTION_ID, PRODUCT_ID, request);

        ResponseEntity<PromotionResponseDTO> response = 
                controller.updatePromotion(PROMOTION_ID, null, request);

        thenPromotionIsUpdatedSuccessfully(response, PROMOTION_ID, useCaseRequest);
    }

    @Test
    void deletePromotionDelegatesAndReturnsNoContent() {
        ResponseEntity<Void> response = controller.deletePromotion(null, PROMOTION_ID);

        thenPromotionIsDeletedSuccessfully(response, PROMOTION_ID);
    }

    private void givenActivePromotionsReturned(UUID promotionId, UUID productId) {
        Promotion promotion = promotion(promotionId, productId);
        when(getActivePromotions.execute(new GetActivePromotionsRequest()))
                .thenReturn(new GetActivePromotionsResponse(
                        List.of(CreatePromotionResponse.fromDomain(promotion))));
    }

    private void givenActivePromotionsByBuffetReturned(
            UUID buffetId, 
            UUID promotionId, 
            UUID productId
    ) {
        Promotion promotion = promotion(promotionId, productId);
        when(getAllPromotionsByBuffet.execute(new GetActivePromotionsByBuffetRequest(buffetId)))
                .thenReturn(new GetActivePromotionsByBuffetResponse(
                        List.of(CreatePromotionResponse.fromDomain(promotion))));
    }

    private void givenPromotionByIdReturned(UUID promotionId, UUID productId) {
        when(getPromotionById.execute(new GetPromotionByIdRequest(promotionId)))
                .thenReturn(CreatePromotionResponse.fromDomain(promotion(promotionId, productId)));
    }

    private PromotionRequestDTO givenPromotionRequest(
            UUID firstProductId, 
            UUID secondProductId, 
            UUID buffetId
    ) {
        PromotionRequestDTO request = new PromotionRequestDTO();
        request.setName("Combo");
        request.setDiscountPercentage(new BigDecimal("15.00"));
        request.setStartDate(LocalDateTime.of(2026, 6, 1, 8, 0));
        request.setEndDate(LocalDateTime.of(2026, 6, 30, 23, 0));
        request.setProductIds(List.of(firstProductId, secondProductId));
        request.setBuffetId(buffetId);
        return request;
    }

    private void givenPromotionCreated(UUID promotionId, UUID productId) {
        Promotion promotion = promotion(promotionId, productId);
        CreatePromotionResponse createResponse = CreatePromotionResponse.fromDomain(promotion);
        when(createPromotion.execute(any(CreatePromotionRequest.class))).thenReturn(createResponse);
    }

    private UpdatePromotionRequest givenPromotionUpdated(
            UUID promotionId, 
            UUID productId, 
            PromotionRequestDTO request
    ) {
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
        return useCaseRequest;
    }

    private void thenListActivePromotionsReturnsCorrectly(
            ResponseEntity<List<PromotionResponseDTO>> response, 
            UUID promotionId, 
            UUID productId
    ) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(promotionId, response.getBody().get(0).getId());
        assertEquals("Combo", response.getBody().get(0).getName());
        assertEquals(List.of(productId), response.getBody().get(0).getProductIds());
    }

    private void thenListActivePromotionsByBuffetReturnsCorrectly(
            ResponseEntity<List<PromotionResponseDTO>> response, 
            UUID buffetId, 
            UUID promotionId
    ) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(promotionId, response.getBody().get(0).getId());
        verify(getAllPromotionsByBuffet).execute(new GetActivePromotionsByBuffetRequest(buffetId));
    }

    private void thenPromotionByIdReturnsCorrectly(
            ResponseEntity<PromotionResponseDTO> response, 
            UUID promotionId
    ) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("ACTIVE", response.getBody().getStatus());
        verify(getPromotionById).execute(new GetPromotionByIdRequest(promotionId));
    }

    private void thenPromotionIsCreatedSuccessfully(
            ResponseEntity<PromotionResponseDTO> response, 
            UUID promotionId
    ) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(promotionId, response.getBody().getId());
        verify(planAccessGuard).requireVendorAdvanced(null);
        verify(createPromotion).execute(any(CreatePromotionRequest.class));
    }

    private void thenPromotionIsUpdatedSuccessfully(
            ResponseEntity<PromotionResponseDTO> response, 
            UUID promotionId, 
            UpdatePromotionRequest useCaseRequest
    ) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(promotionId, response.getBody().getId());
        verify(planAccessGuard).requireVendorAdvanced(null);
        verify(updatePromotion).execute(useCaseRequest);
    }

    private void thenPromotionIsDeletedSuccessfully(
            ResponseEntity<Void> response, 
            UUID promotionId
    ) {
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(planAccessGuard).requireVendorAdvanced(null);
        verify(deletePromotion).execute(new DeletePromotionRequest(promotionId));
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
