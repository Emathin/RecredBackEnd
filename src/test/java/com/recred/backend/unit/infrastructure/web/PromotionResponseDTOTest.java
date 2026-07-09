package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.CreatePromotionResponse;
import com.recred.backend.application.PromotionResponse;
import com.recred.backend.application.SeasonalCreatedPromotionResponse;
import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.infrastructure.web.PromotionResponseDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PromotionResponseDTOTest {

    @Test
    void fromAppResponseShouldMapAllFields() {
        UUID promotionId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        LocalDateTime startDate = LocalDateTime.of(2026, 7, 1, 8, 0);
        LocalDateTime endDate = LocalDateTime.of(2026, 7, 10, 18, 0);
        PromotionResponse response = new PromotionResponse(
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

        PromotionResponseDTO dto = PromotionResponseDTO.fromAppResponse(response);

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

    @Test
    void fromDomainShouldMapProductsBuffetAndMissingOptionalRelations() {
        UUID productId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Promotion promotion = promotion();
        promotion.setProducts(List.of(product(productId)));
        promotion.setBuffet(buffet(buffetId));

        PromotionResponseDTO dto = PromotionResponseDTO.fromDomain(promotion);

        assertEquals(List.of(productId), dto.getProductIds());
        assertEquals(buffetId, dto.getBuffetId());

        promotion.setProducts(null);
        promotion.setBuffet(null);
        PromotionResponseDTO withoutRelations = PromotionResponseDTO.fromDomain(promotion);

        assertNull(withoutRelations.getProductIds());
        assertNull(withoutRelations.getBuffetId());
    }

    @Test
    void fromCreatePromotionResponseShouldMapAllFields() {
        UUID productId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        CreatePromotionResponse response = new CreatePromotionResponse(
                UUID.randomUUID(),
                "Promo merienda",
                new BigDecimal("20.00"),
                LocalDateTime.of(2026, 7, 1, 8, 0),
                LocalDateTime.of(2026, 7, 10, 18, 0),
                PromotionStatus.INACTIVE,
                "https://example.com/merienda.png",
                buffetId,
                List.of(productId)
        );

        PromotionResponseDTO dto = PromotionResponseDTO.fromApplication(response);

        assertEquals(response.id(), dto.getId());
        assertEquals("DRAFT", dto.getStatus());
        assertEquals(List.of(productId), dto.getProductIds());
        assertEquals(buffetId, dto.getBuffetId());
        assertEquals(response.imageUrl(), dto.getImageUrl());
    }

    @Test
    void fromSeasonalPromotionResponseShouldMapAllFieldsAndNullInput() {
        UUID productId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        SeasonalCreatedPromotionResponse response = new SeasonalCreatedPromotionResponse(
                UUID.randomUUID(),
                "Promo invierno",
                new BigDecimal("25.00"),
                LocalDateTime.of(2026, 7, 1, 8, 0),
                LocalDateTime.of(2026, 7, 10, 18, 0),
                "ACTIVE",
                List.of(productId),
                buffetId,
                "https://example.com/invierno.png"
        );

        PromotionResponseDTO dto = PromotionResponseDTO.fromApplication(response);

        assertEquals(response.id(), dto.getId());
        assertEquals("ACTIVE", dto.getStatus());
        assertEquals(List.of(productId), dto.getProductIds());
        assertEquals(buffetId, dto.getBuffetId());
        assertEquals(response.imageUrl(), dto.getImageUrl());
        assertNull(PromotionResponseDTO.fromApplication((SeasonalCreatedPromotionResponse) null));
    }

    private Promotion promotion() {
        Promotion promotion = new Promotion();
        promotion.setId(UUID.randomUUID());
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
