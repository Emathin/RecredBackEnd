package com.recred.backend.unit.application;

import com.recred.backend.application.PromotionResponse;
import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PromotionResponseTest {

    @Test
    void fromDomainShouldMapPromotionWithProductsAndBuffet() {
        UUID promotionId = UUID.randomUUID();
        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        LocalDateTime startDate = LocalDateTime.of(2026, 7, 1, 8, 0);
        LocalDateTime endDate = LocalDateTime.of(2026, 7, 31, 18, 0);
        Promotion promotion = promotion(promotionId, startDate, endDate);
        promotion.setStatus(PromotionStatus.ACTIVE);
        promotion.setProducts(List.of(product(firstProductId), product(secondProductId)));
        promotion.setBuffet(buffet(buffetId));

        PromotionResponse response = PromotionResponse.fromDomain(promotion);

        assertEquals(promotionId, response.id());
        assertEquals("Combo desayuno", response.name());
        assertEquals(new BigDecimal("15.00"), response.discountPercentage());
        assertEquals(startDate, response.startDate());
        assertEquals(endDate, response.endDate());
        assertEquals("ACTIVE", response.status());
        assertEquals(List.of(firstProductId, secondProductId), response.productIds());
        assertEquals(buffetId, response.buffetId());
        assertEquals("https://img.test/promo.png", response.imageUrl());
    }

    @Test
    void fromDomainShouldKeepNullableAssociationsAsNull() {
        Promotion promotion = promotion(
                UUID.randomUUID(),
                LocalDateTime.of(2026, 7, 1, 8, 0),
                LocalDateTime.of(2026, 7, 31, 18, 0)
        );

        PromotionResponse response = PromotionResponse.fromDomain(promotion);

        assertNull(response.status());
        assertNull(response.productIds());
        assertNull(response.buffetId());
    }

    private Promotion promotion(
            UUID promotionId,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        Promotion promotion = new Promotion();
        promotion.setId(promotionId);
        promotion.setName("Combo desayuno");
        promotion.setDiscountPercentage(new BigDecimal("15.00"));
        promotion.setStartDate(startDate);
        promotion.setEndDate(endDate);
        promotion.setImageUrl("https://img.test/promo.png");
        return promotion;
    }

    private Producto product(UUID productId) {
        Producto product = new Producto();
        product.setId(productId);
        return product;
    }

    private Buffet buffet(UUID buffetId) {
        Buffet buffet = new Buffet();
        buffet.setId(buffetId);
        return buffet;
    }
}
