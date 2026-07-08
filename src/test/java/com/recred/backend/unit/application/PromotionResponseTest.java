package com.recred.backend.unit.application;

import com.recred.backend.application.PromotionResponse;
import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PromotionResponseTest {

    private UUID promotionId;
    private UUID firstProductId;
    private UUID secondProductId;
    private UUID buffetId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Promotion promotion;
    private PromotionResponse response;

    @Test
    void fromDomain_WhenPromotionHasProductsAndBuffet_MapsPromotionData() {
        givenPromotionWithProductsAndBuffet();

        whenMappingPromotionResponse();

        thenPromotionDataWasMapped();
        thenPromotionRelationsWereMapped();
    }

    @Test
    void fromDomain_WhenPromotionHasNoOptionalRelations_KeepsRelationsAsNull() {
        givenPromotionWithoutOptionalRelations();

        whenMappingPromotionResponse();

        thenOptionalRelationsWereKeptAsNull();
    }

    private void givenPromotionWithProductsAndBuffet() {
        givenPromotionWithoutOptionalRelations();
        promotion.setStatus(PromotionStatus.ACTIVE);

        firstProductId = randomUUID();
        secondProductId = randomUUID();
        buffetId = randomUUID();

        promotion.setProducts(List.of(product(firstProductId), product(secondProductId)));
        promotion.setBuffet(buffet(buffetId));
    }

    private void givenPromotionWithoutOptionalRelations() {
        promotionId = randomUUID();
        startDate = LocalDateTime.of(2026, 7, 1, 8, 0);
        endDate = LocalDateTime.of(2026, 7, 31, 18, 0);
        promotion = promotion();
    }

    private void whenMappingPromotionResponse() {
        response = PromotionResponse.fromDomain(promotion);
    }

    private void thenPromotionDataWasMapped() {
        assertEquals(promotionId, response.id());
        assertEquals("Combo desayuno", response.name());
        assertEquals(new BigDecimal("15.00"), response.discountPercentage());
        assertEquals(startDate, response.startDate());
        assertEquals(endDate, response.endDate());
        assertEquals("ACTIVE", response.status());
        assertEquals("https://img.test/promo.png", response.imageUrl());
    }

    private void thenPromotionRelationsWereMapped() {
        assertEquals(List.of(firstProductId, secondProductId), response.productIds());
        assertEquals(buffetId, response.buffetId());
    }

    private void thenOptionalRelationsWereKeptAsNull() {
        assertNull(response.status());
        assertNull(response.productIds());
        assertNull(response.buffetId());
    }

    private Promotion promotion() {
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
