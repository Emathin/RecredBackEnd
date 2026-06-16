package com.recred.backend.unit.application;

import com.recred.backend.application.CreatePromotion;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.domain.repositories.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatePromotionTest {

    @Mock
    private PromotionRepository promotionRepository;

    @InjectMocks
    private CreatePromotion createPromotion;

    @Test
    void execute_shouldCreatePromotionWithDraftStatus_whenForceDraftIsTrue() {
        UUID prod1 = UUID.randomUUID();
        UUID prod2 = UUID.randomUUID();
        List<UUID> productIds = List.of(prod1, prod2);
        BigDecimal discount = new BigDecimal("20");
        String name = "Promo Test";
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(5);

        when(promotionRepository.save(any(Promotion.class))).thenAnswer(i -> i.getArguments()[0]);

        Promotion result = createPromotion.execute(productIds, discount, name, start, end, true);

        assertNotNull(result);
        assertEquals(name, result.getName());
        assertEquals(discount, result.getDiscountPercentage());
        assertEquals(start, result.getStartDate());
        assertEquals(end, result.getEndDate());
        assertEquals(PromotionStatus.DRAFT, result.getStatus());
        assertEquals(2, result.getProducts().size());
        assertEquals(prod1, result.getProducts().get(0).getId());
        assertEquals(prod2, result.getProducts().get(1).getId());

        verify(promotionRepository).save(any(Promotion.class));
    }

    @Test
    void execute_shouldCreatePromotionWithActiveStatus_whenForceDraftIsFalseAndNowIsWithinDates() {
        List<UUID> productIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        BigDecimal discount = new BigDecimal("15");
        String name = "Promo Active";
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(2);

        when(promotionRepository.save(any(Promotion.class))).thenAnswer(i -> i.getArguments()[0]);

        Promotion result = createPromotion.execute(productIds, discount, name, start, end, false);

        assertNotNull(result);
        assertEquals(PromotionStatus.ACTIVE, result.getStatus());
        verify(promotionRepository).save(any(Promotion.class));
    }

    @Test
    void execute_shouldCreatePromotionWithDraftStatus_whenForceDraftIsFalseAndNowIsOutsideDates() {
        List<UUID> productIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        BigDecimal discount = new BigDecimal("15");
        String name = "Promo Future";
        LocalDateTime start = LocalDateTime.now().plusDays(5);
        LocalDateTime end = LocalDateTime.now().plusDays(10);

        when(promotionRepository.save(any(Promotion.class))).thenAnswer(i -> i.getArguments()[0]);

        Promotion result = createPromotion.execute(productIds, discount, name, start, end, false);

        assertNotNull(result);
        assertEquals(PromotionStatus.DRAFT, result.getStatus());
        verify(promotionRepository).save(any(Promotion.class));
    }

    @Test
    void execute_shouldThrowException_whenLessThanTwoProducts() {
        List<UUID> productIds = List.of(UUID.randomUUID());
        BigDecimal discount = new BigDecimal("15");
        String name = "Promo Fail";
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                createPromotion.execute(productIds, discount, name, start, end, false));

        assertEquals("A promotion requires at least 2 products.", ex.getMessage());
        verify(promotionRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenDiscountPercentageIsInvalid() {
        List<UUID> productIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        String name = "Promo Fail";
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        // Test <= 0
        assertThrows(IllegalArgumentException.class, () ->
                createPromotion.execute(productIds, BigDecimal.ZERO, name, start, end, false));
        assertThrows(IllegalArgumentException.class, () ->
                createPromotion.execute(productIds, new BigDecimal("-5"), name, start, end, false));

        // Test > 100
        assertThrows(IllegalArgumentException.class, () ->
                createPromotion.execute(productIds, new BigDecimal("101"), name, start, end, false));

        // Test null
        assertThrows(IllegalArgumentException.class, () ->
                createPromotion.execute(productIds, null, name, start, end, false));

        verify(promotionRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenDateRangeIsInvalid() {
        List<UUID> productIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        BigDecimal discount = new BigDecimal("15");
        String name = "Promo Fail";
        LocalDateTime start = LocalDateTime.now().plusDays(5);
        LocalDateTime end = LocalDateTime.now().plusDays(2); // End before start

        assertThrows(IllegalArgumentException.class, () ->
                createPromotion.execute(productIds, discount, name, start, end, false));

        // Test null dates
        assertThrows(IllegalArgumentException.class, () ->
                createPromotion.execute(productIds, discount, name, null, end, false));
        assertThrows(IllegalArgumentException.class, () ->
                createPromotion.execute(productIds, discount, name, start, null, false));

        verify(promotionRepository, never()).save(any());
    }
}
