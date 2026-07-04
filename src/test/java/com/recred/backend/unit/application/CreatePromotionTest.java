package com.recred.backend.unit.application;

import com.recred.backend.application.CreatePromotion;
import com.recred.backend.application.CreatePromotionRequest;
import com.recred.backend.application.CreatePromotionResponse;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.domain.repositories.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
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
        CreatePromotionRequest request = request(
                productIds,
                discount,
                name,
                start,
                end,
                true,
                null
        );

        when(promotionRepository.save(any(Promotion.class))).thenAnswer(i -> i.getArguments()[0]);

        CreatePromotionResponse result = createPromotion.execute(request);

        assertNotNull(result);
        assertEquals(name, result.name());
        assertEquals(discount, result.discountPercentage());
        assertEquals(start, result.startDate());
        assertEquals(end, result.endDate());
        assertEquals(PromotionStatus.DRAFT, result.status());
        assertEquals(2, result.productIds().size());
        assertEquals(prod1, result.productIds().get(0));
        assertEquals(prod2, result.productIds().get(1));

        verify(promotionRepository).save(any(Promotion.class));
    }

    @Test
    void execute_shouldCreatePromotionWithActiveStatus_whenForceDraftIsFalseAndNowIsWithinDates() {
        List<UUID> productIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        BigDecimal discount = new BigDecimal("15");
        String name = "Promo Active";
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(2);
        CreatePromotionRequest request = request(
                productIds,
                discount,
                name,
                start,
                end,
                false,
                null
        );

        when(promotionRepository.save(any(Promotion.class))).thenAnswer(i -> i.getArguments()[0]);

        CreatePromotionResponse result = createPromotion.execute(request);

        assertNotNull(result);
        assertEquals(PromotionStatus.ACTIVE, result.status());
        verify(promotionRepository).save(any(Promotion.class));
    }

    @Test
    void execute_shouldCreatePromotionWithDraftStatus_whenForceDraftIsFalseAndNowIsOutsideDates() {
        List<UUID> productIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        BigDecimal discount = new BigDecimal("15");
        String name = "Promo Future";
        LocalDateTime start = LocalDateTime.now().plusDays(5);
        LocalDateTime end = LocalDateTime.now().plusDays(10);
        CreatePromotionRequest request = request(
                productIds,
                discount,
                name,
                start,
                end,
                false,
                null
        );

        when(promotionRepository.save(any(Promotion.class))).thenAnswer(i -> i.getArguments()[0]);

        CreatePromotionResponse result = createPromotion.execute(request);

        assertNotNull(result);
        assertEquals(PromotionStatus.DRAFT, result.status());
        verify(promotionRepository).save(any(Promotion.class));
    }

    @Test
    void execute_shouldThrowException_whenLessThanTwoProducts() {
        List<UUID> productIds = List.of(UUID.randomUUID());
        BigDecimal discount = new BigDecimal("15");
        String name = "Promo Fail";
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        CreatePromotionRequest request = request(
                productIds,
                discount,
                name,
                start,
                end,
                false,
                null
        );

        Executable execution = () -> createPromotion.execute(request);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, execution);

        assertEquals("A promotion requires at least 2 products.", ex.getMessage());
        verify(promotionRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenDiscountPercentageIsZero() {
        List<UUID> productIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        String name = "Promo Fail";
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        CreatePromotionRequest request = request(
                productIds,
                BigDecimal.ZERO,
                name,
                start,
                end,
                false,
                null
        );

        Executable execution = () -> createPromotion.execute(request);

        assertThrows(IllegalArgumentException.class, execution);

        verify(promotionRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenDiscountPercentageIsNegative() {
        List<UUID> productIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        String name = "Promo Fail";
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        CreatePromotionRequest request = request(
                productIds,
                new BigDecimal("-5"),
                name,
                start,
                end,
                false,
                null
        );

        Executable execution = () -> createPromotion.execute(request);

        assertThrows(IllegalArgumentException.class, execution);

        verify(promotionRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenDiscountPercentageIsGreaterThan100() {
        List<UUID> productIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        String name = "Promo Fail";
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        CreatePromotionRequest request = request(
                productIds,
                new BigDecimal("101"),
                name,
                start,
                end,
                false,
                null
        );

        Executable execution = () -> createPromotion.execute(request);

        assertThrows(IllegalArgumentException.class, execution);

        verify(promotionRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenDiscountPercentageIsNull() {
        List<UUID> productIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        String name = "Promo Fail";
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        CreatePromotionRequest request = request(
                productIds,
                null,
                name,
                start,
                end,
                false,
                null
        );

        Executable execution = () -> createPromotion.execute(request);

        assertThrows(IllegalArgumentException.class, execution);

        verify(promotionRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenStartDateIsAfterEndDate() {
        List<UUID> productIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        BigDecimal discount = new BigDecimal("15");
        String name = "Promo Fail";
        LocalDateTime start = LocalDateTime.now().plusDays(5);
        LocalDateTime end = LocalDateTime.now().plusDays(2);
        CreatePromotionRequest request = request(
                productIds,
                discount,
                name,
                start,
                end,
                false,
                null
        );

        Executable execution = () -> createPromotion.execute(request);

        assertThrows(IllegalArgumentException.class, execution);

        verify(promotionRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenStartDateIsNull() {
        List<UUID> productIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        BigDecimal discount = new BigDecimal("15");
        String name = "Promo Fail";
        LocalDateTime end = LocalDateTime.now().plusDays(2);
        CreatePromotionRequest request = request(
                productIds,
                discount,
                name,
                null,
                end,
                false,
                null
        );

        Executable execution = () -> createPromotion.execute(request);

        assertThrows(IllegalArgumentException.class, execution);

        verify(promotionRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenEndDateIsNull() {
        List<UUID> productIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        BigDecimal discount = new BigDecimal("15");
        String name = "Promo Fail";
        LocalDateTime start = LocalDateTime.now().plusDays(5);
        CreatePromotionRequest request = request(
                productIds,
                discount,
                name,
                start,
                null,
                false,
                null
        );

        Executable execution = () -> createPromotion.execute(request);

        assertThrows(IllegalArgumentException.class, execution);

        verify(promotionRepository, never()).save(any());
    }

    @Test
    void execute_shouldCreatePromotionWithImageUrl_whenImageUrlIsProvided() {
        List<UUID> productIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        BigDecimal discount = new BigDecimal("15");
        String name = "Promo Image";
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(2);
        String imageUrl = "https://res.cloudinary.com/djzfudbze/image/upload/v1/collage.png";
        CreatePromotionRequest request = request(
                productIds,
                discount,
                name,
                start,
                end,
                false,
                imageUrl
        );

        when(promotionRepository.save(any(Promotion.class))).thenAnswer(i -> i.getArguments()[0]);

        CreatePromotionResponse result = createPromotion.execute(request);

        assertNotNull(result);
        assertEquals(imageUrl, result.imageUrl());
        verify(promotionRepository).save(any(Promotion.class));
    }

    private CreatePromotionRequest request(
            List<UUID> productIds,
            BigDecimal discount,
            String name,
            LocalDateTime start,
            LocalDateTime end,
            boolean forceDraft,
            String imageUrl
    ) {
        return new CreatePromotionRequest(
                productIds,
                discount,
                name,
                start,
                end,
                null,
                forceDraft,
                imageUrl
        );
    }
}
