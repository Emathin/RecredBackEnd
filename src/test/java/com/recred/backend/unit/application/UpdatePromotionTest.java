package com.recred.backend.unit.application;

import com.recred.backend.application.UpdatePromotion;
import com.recred.backend.application.UpdatePromotionRequest;
import com.recred.backend.application.CreatePromotionResponse;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.domain.repositories.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdatePromotionTest {

    @Mock
    private PromotionRepository promotionRepository;

    @InjectMocks
    private UpdatePromotion updatePromotion;

    private UUID promoId;
    private Promotion existingPromotion;

    @BeforeEach
    void setUp() {
        promoId = UUID.randomUUID();
        existingPromotion = new Promotion();
        existingPromotion.setId(promoId);
        existingPromotion.setName("Old Promo");
        existingPromotion.setDiscountPercentage(new BigDecimal("10"));
        existingPromotion.setStartDate(LocalDateTime.now().minusDays(5));
        existingPromotion.setEndDate(LocalDateTime.now().plusDays(5));
        existingPromotion.setStatus(PromotionStatus.ACTIVE);
        
        Producto p1 = new Producto();
        p1.setId(UUID.randomUUID());
        Producto p2 = new Producto();
        p2.setId(UUID.randomUUID());
        existingPromotion.setProducts(List.of(p1, p2));
    }

    @Test
    void execute_shouldUpdatePromotion_whenPromotionExistsAndAllFieldsValid() {
        UUID newProd1 = UUID.randomUUID();
        UUID newProd2 = UUID.randomUUID();
        List<UUID> newProductIds = List.of(newProd1, newProd2);
        BigDecimal newDiscount = new BigDecimal("25");
        String newName = "New Name";
        LocalDateTime newStart = LocalDateTime.now().minusDays(1);
        LocalDateTime newEnd = LocalDateTime.now().plusDays(10);

        when(promotionRepository.findById(promoId)).thenReturn(Optional.of(existingPromotion));
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(i -> i.getArguments()[0]);

        CreatePromotionResponse result = updatePromotion.execute(new UpdatePromotionRequest(
                promoId, newProductIds, newDiscount, newName, newStart, newEnd));

        assertNotNull(result);
        assertEquals(newName, result.name());
        assertEquals(newDiscount, result.discountPercentage());
        assertEquals(newStart, result.startDate());
        assertEquals(newEnd, result.endDate());
        assertEquals(PromotionStatus.ACTIVE, result.status());
        assertEquals(2, result.productIds().size());
        assertEquals(newProd1, result.productIds().get(0));
        assertEquals(newProd2, result.productIds().get(1));

        verify(promotionRepository).save(any(Promotion.class));
    }

    @Test
    void execute_shouldUpdatePromotionPartially_whenPromotionExistsAndSomeFieldsNull() {
        when(promotionRepository.findById(promoId)).thenReturn(Optional.of(existingPromotion));
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(i -> i.getArguments()[0]);

        CreatePromotionResponse result = updatePromotion.execute(new UpdatePromotionRequest(
                promoId, null, null, "Updated Title Only", null, null));

        assertNotNull(result);
        assertEquals("Updated Title Only", result.name());
        assertEquals(new BigDecimal("10"), result.discountPercentage());
        assertEquals(existingPromotion.getStartDate(), result.startDate());
        verify(promotionRepository).save(any(Promotion.class));
    }

    @Test
    void execute_shouldSetDraftStatus_whenPromotionStartsInFuture() {
        LocalDateTime futureStart = LocalDateTime.now().plusDays(1);
        LocalDateTime futureEnd = LocalDateTime.now().plusDays(10);
        when(promotionRepository.findById(promoId)).thenReturn(Optional.of(existingPromotion));
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(i -> i.getArguments()[0]);

        CreatePromotionResponse result = updatePromotion.execute(new UpdatePromotionRequest(
                promoId,
                null,
                null,
                null,
                futureStart,
                futureEnd
        ));

        assertEquals(PromotionStatus.DRAFT, result.status());
    }

    @Test
    void execute_shouldThrowException_whenPromotionNotFound() {
        when(promotionRepository.findById(promoId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                updatePromotion.execute(new UpdatePromotionRequest(
                        promoId, null, null, null, null, null)));

        verify(promotionRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenUpdatedProductsLessThanTwo() {
        when(promotionRepository.findById(promoId)).thenReturn(Optional.of(existingPromotion));

        List<UUID> invalidProducts = List.of(UUID.randomUUID());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                updatePromotion.execute(new UpdatePromotionRequest(
                        promoId, invalidProducts, null, null, null, null)));

        assertEquals("A promotion requires at least 2 products.", ex.getMessage());
        verify(promotionRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenUpdatedDiscountIsInvalid() {
        when(promotionRepository.findById(promoId)).thenReturn(Optional.of(existingPromotion));

        assertThrows(IllegalArgumentException.class, () ->
                updatePromotion.execute(new UpdatePromotionRequest(
                        promoId, null, BigDecimal.ZERO, null, null, null)));
        assertThrows(IllegalArgumentException.class, () ->
                updatePromotion.execute(new UpdatePromotionRequest(
                        promoId, null, new BigDecimal("105"), null, null, null)));

        verify(promotionRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowException_whenUpdatedDatesAreInvalid() {
        when(promotionRepository.findById(promoId)).thenReturn(Optional.of(existingPromotion));

        LocalDateTime badStart = LocalDateTime.now().plusDays(10);
        LocalDateTime badEnd = LocalDateTime.now().plusDays(2);

        assertThrows(IllegalArgumentException.class, () ->
                updatePromotion.execute(new UpdatePromotionRequest(
                        promoId, null, null, null, badStart, badEnd)));

        verify(promotionRepository, never()).save(any());
    }
}
