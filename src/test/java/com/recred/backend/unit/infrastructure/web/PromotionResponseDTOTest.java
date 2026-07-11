package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.CreatePromotionResponse;
import com.recred.backend.application.PromotionResponse;
import com.recred.backend.application.SeasonalCreatedPromotionResponse;
import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.infrastructure.web.PromotionResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PromotionResponseDTOTest {

    private UUID promotionId;
    private UUID productId;
    private UUID buffetId;

    @BeforeEach
    void setUp() {
        promotionId = UUID.randomUUID();
        productId = UUID.randomUUID();
        buffetId = UUID.randomUUID();
    }

    @Test
    void fromAppResponseShouldMapAllFields() {
        PromotionResponse response = givenPromotionResponse();

        PromotionResponseDTO dto = PromotionResponseDTO.fromAppResponse(response);

        thenMapsPromotionResponseCorrectly(dto);
    }

    @Test
    void fromDomainShouldMapProductsBuffetAndMissingOptionalRelations() {
        Promotion promotion = givenDomainPromotionWithRelations();
        Promotion promotionWithoutRelations = givenDomainPromotionWithoutRelations();

        PromotionResponseDTO dto = PromotionResponseDTO.fromDomain(promotion);
        PromotionResponseDTO withoutRelations = PromotionResponseDTO.fromDomain(promotionWithoutRelations);

        thenMapsDomainPromotionCorrectly(dto);
        thenMapsDomainPromotionWithoutRelationsCorrectly(withoutRelations);
    }

    @Test
    void fromCreatePromotionResponseShouldMapAllFields() {
        CreatePromotionResponse response = givenCreatePromotionResponse();

        PromotionResponseDTO dto = PromotionResponseDTO.fromApplication(response);

        thenMapsCreatePromotionResponseCorrectly(response, dto);
    }

    @Test
    void fromSeasonalPromotionResponseShouldMapAllFieldsAndNullInput() {
        SeasonalCreatedPromotionResponse response = givenSeasonalPromotionResponse();

        PromotionResponseDTO dto = PromotionResponseDTO.fromApplication(response);
        PromotionResponseDTO nullDto = PromotionResponseDTO.fromApplication((SeasonalCreatedPromotionResponse) null);

        thenMapsSeasonalPromotionResponseCorrectly(response, dto, nullDto);
    }

    private PromotionResponse givenPromotionResponse() {
        LocalDateTime startDate = LocalDateTime.of(2026, 7, 1, 8, 0);
        LocalDateTime endDate = LocalDateTime.of(2026, 7, 10, 18, 0);
        return new PromotionResponse(
                promotionId,
                "Promo desayuno",
                new BigDecimal("15.00"),
                startDate,
                endDate,
                "ACTIVE",
                List.of(productId),
                buffetId,
                "https://example.com/promo.png"
        );
    }

    private Promotion givenDomainPromotionWithRelations() {
        Promotion promotion = promotion();
        promotion.setProducts(List.of(product(productId)));
        promotion.setBuffet(buffet(buffetId));
        return promotion;
    }

    private Promotion givenDomainPromotionWithoutRelations() {
        Promotion promotion = promotion();
        promotion.setProducts(null);
        promotion.setBuffet(null);
        return promotion;
    }

    private CreatePromotionResponse givenCreatePromotionResponse() {
        return new CreatePromotionResponse(
                promotionId,
                "Promo merienda",
                new BigDecimal("20.00"),
                LocalDateTime.of(2026, 7, 1, 8, 0),
                LocalDateTime.of(2026, 7, 10, 18, 0),
                PromotionStatus.INACTIVE,
                "https://example.com/merienda.png",
                buffetId,
                List.of(productId)
        );
    }

    private SeasonalCreatedPromotionResponse givenSeasonalPromotionResponse() {
        return new SeasonalCreatedPromotionResponse(
                promotionId,
                "Promo invierno",
                new BigDecimal("25.00"),
                LocalDateTime.of(2026, 7, 1, 8, 0),
                LocalDateTime.of(2026, 7, 10, 18, 0),
                "ACTIVE",
                List.of(productId),
                buffetId,
                "https://example.com/invierno.png"
        );
    }

    private void thenMapsPromotionResponseCorrectly(PromotionResponseDTO dto) {
        LocalDateTime startDate = LocalDateTime.of(2026, 7, 1, 8, 0);
        LocalDateTime endDate = LocalDateTime.of(2026, 7, 10, 18, 0);
        assertEquals(promotionId, dto.getId());
        assertEquals("Promo desayuno", dto.getName());
        assertEquals(new BigDecimal("15.00"), dto.getDiscountPercentage());
        assertEquals(startDate, dto.getStartDate());
        assertEquals(endDate, dto.getEndDate());
        assertEquals("ACTIVE", dto.getStatus());
        assertEquals(List.of(productId), dto.getProductIds());
        assertEquals(buffetId, dto.getBuffetId());
        assertEquals("https://example.com/promo.png", dto.getImageUrl());
    }

    private void thenMapsDomainPromotionCorrectly(PromotionResponseDTO dto) {
        assertEquals(List.of(productId), dto.getProductIds());
        assertEquals(buffetId, dto.getBuffetId());
    }

    private void thenMapsDomainPromotionWithoutRelationsCorrectly(PromotionResponseDTO dto) {
        assertNull(dto.getProductIds());
        assertNull(dto.getBuffetId());
    }

    private void thenMapsCreatePromotionResponseCorrectly(CreatePromotionResponse response, PromotionResponseDTO dto) {
        assertEquals(response.id(), dto.getId());
        assertEquals("INACTIVE", dto.getStatus());
        assertEquals(List.of(productId), dto.getProductIds());
        assertEquals(buffetId, dto.getBuffetId());
        assertEquals(response.imageUrl(), dto.getImageUrl());
    }

    private void thenMapsSeasonalPromotionResponseCorrectly(
            SeasonalCreatedPromotionResponse response, 
            PromotionResponseDTO dto, 
            PromotionResponseDTO nullDto
    ) {
        assertEquals(response.id(), dto.getId());
        assertEquals("ACTIVE", dto.getStatus());
        assertEquals(List.of(productId), dto.getProductIds());
        assertEquals(buffetId, dto.getBuffetId());
        assertEquals(response.imageUrl(), dto.getImageUrl());
        assertNull(nullDto);
    }

    private Promotion promotion() {
        Promotion promotion = new Promotion();
        promotion.setId(promotionId);
        promotion.setName("Promo desayuno");
        promotion.setDiscountPercentage(new BigDecimal("15.00"));
        promotion.setStartDate(LocalDateTime.of(2026, 7, 1, 8, 0));
        promotion.setEndDate(LocalDateTime.of(2026, 7, 10, 18, 0));
        promotion.setStatus(PromotionStatus.ACTIVE);
        promotion.setImageUrl("https://example.com/promo.png");
        return promotion;
    }

    private Producto product(UUID id) {
        Producto product = new Producto();
        product.setId(id);
        return product;
    }

    private Buffet buffet(UUID id) {
        Buffet buffet = new Buffet();
        buffet.setId(id);
        return buffet;
    }
}
