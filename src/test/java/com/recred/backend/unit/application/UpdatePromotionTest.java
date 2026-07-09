package com.recred.backend.unit.application;

import com.recred.backend.application.CreatePromotionResponse;
import com.recred.backend.application.UpdatePromotion;
import com.recred.backend.application.UpdatePromotionRequest;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.domain.repositories.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdatePromotionTest {

    @Mock
    private PromotionRepository promotionRepository;

    private UpdatePromotion useCase;

    private UUID promotionId;
    private UUID firstProductId;
    private UUID secondProductId;
    private Promotion existingPromotion;
    private UpdatePromotionRequest request;
    private CreatePromotionResponse response;
    private RuntimeException thrownException;

    @BeforeEach
    void setUp() {
        useCase = new UpdatePromotion(promotionRepository);
    }

    @Test
    void givenPromotionExists_whenUpdatingAllFields_thenSavesUpdatedPromotion() {
        givenPromotionUpdateScenario();
        givenExistingPromotion();
        givenCompletePromotionUpdateRequest();
        givenPromotionExists();
        givenPromotionCanBeSaved();

        whenUpdatingPromotion();

        thenPromotionResponseWasReturned();
        thenPromotionFieldsWereUpdated();
        thenPromotionProductsWereUpdated();
        thenPromotionWasSaved();
    }

    @Test
    void givenPromotionExists_whenUpdatingOnlyName_thenKeepsPreviousValues() {
        givenPromotionUpdateScenario();
        givenExistingPromotion();
        givenNameOnlyUpdateRequest();
        givenPromotionExists();
        givenPromotionCanBeSaved();

        whenUpdatingPromotion();

        thenPromotionNameIs("Updated Title Only");
        thenDiscountIs("10");
        thenStartDateWasKept();
        thenPromotionWasSaved();
    }

    @Test
    void givenPromotionStartsInFuture_whenUpdatingPromotion_thenSetsDraftStatus() {
        givenPromotionUpdateScenario();
        givenExistingPromotion();
        givenFutureDatesUpdateRequest();
        givenPromotionExists();
        givenPromotionCanBeSaved();

        whenUpdatingPromotion();

        thenPromotionStatusIs(PromotionStatus.INACTIVE);
    }

    @Test
    void givenPromotionDoesNotExist_whenUpdatingPromotion_thenFailsBeforeSaving() {
        givenPromotionUpdateScenario();
        givenEmptyPromotionUpdateRequest();
        givenPromotionDoesNotExist();

        whenUpdatingPromotionFails();

        thenPromotionWasNotFound();
        thenPromotionWasNotSaved();
    }

    @Test
    void givenUpdatedProductsAreLessThanTwo_whenUpdatingPromotion_thenFailsBeforeSaving() {
        givenPromotionUpdateScenario();
        givenExistingPromotion();
        givenInvalidProductsUpdateRequest();
        givenPromotionExists();

        whenUpdatingPromotionFails();

        thenPromotionRequiresAtLeastTwoProducts();
        thenPromotionWasNotSaved();
    }

    @Test
    void givenUpdatedDiscountIsZero_whenUpdatingPromotion_thenFailsBeforeSaving() {
        givenPromotionUpdateScenario();
        givenExistingPromotion();
        givenDiscountUpdateRequest(BigDecimal.ZERO);
        givenPromotionExists();

        whenUpdatingPromotionFails();

        thenPromotionUpdateIsInvalid();
        thenPromotionWasNotSaved();
    }

    @Test
    void givenUpdatedDiscountIsGreaterThan100_whenUpdatingPromotion_thenFailsBeforeSaving() {
        givenPromotionUpdateScenario();
        givenExistingPromotion();
        givenDiscountUpdateRequest(new BigDecimal("105"));
        givenPromotionExists();

        whenUpdatingPromotionFails();

        thenPromotionUpdateIsInvalid();
        thenPromotionWasNotSaved();
    }

    @Test
    void givenUpdatedDatesAreInvalid_whenUpdatingPromotion_thenFailsBeforeSaving() {
        givenPromotionUpdateScenario();
        givenExistingPromotion();
        givenInvalidDatesUpdateRequest();
        givenPromotionExists();

        whenUpdatingPromotionFails();

        thenPromotionUpdateIsInvalid();
        thenPromotionWasNotSaved();
    }

    private void givenPromotionUpdateScenario() {
        promotionId = UUID.randomUUID();
        firstProductId = UUID.randomUUID();
        secondProductId = UUID.randomUUID();
    }

    private void givenExistingPromotion() {
        existingPromotion = new Promotion();
        existingPromotion.setId(promotionId);
        existingPromotion.setName("Old Promo");
        existingPromotion.setDiscountPercentage(new BigDecimal("10"));
        existingPromotion.setStartDate(LocalDateTime.now().minusDays(5));
        existingPromotion.setEndDate(LocalDateTime.now().plusDays(5));
        existingPromotion.setStatus(PromotionStatus.ACTIVE);
        existingPromotion.setProducts(List.of(
                product(UUID.randomUUID()),
                product(UUID.randomUUID())
        ));
    }

    private void givenCompletePromotionUpdateRequest() {
        request = new UpdatePromotionRequest(
                promotionId,
                List.of(firstProductId, secondProductId),
                new BigDecimal("25"),
                "New Name",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(10)
        );
    }

    private void givenNameOnlyUpdateRequest() {
        request = new UpdatePromotionRequest(
                promotionId,
                null,
                null,
                "Updated Title Only",
                null,
                null
        );
    }

    private void givenFutureDatesUpdateRequest() {
        request = new UpdatePromotionRequest(
                promotionId,
                null,
                null,
                null,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(10)
        );
    }

    private void givenEmptyPromotionUpdateRequest() {
        request = new UpdatePromotionRequest(promotionId, null, null, null, null, null);
    }

    private void givenInvalidProductsUpdateRequest() {
        request = new UpdatePromotionRequest(
                promotionId,
                List.of(UUID.randomUUID()),
                null,
                null,
                null,
                null
        );
    }

    private void givenDiscountUpdateRequest(BigDecimal discount) {
        request = new UpdatePromotionRequest(promotionId, null, discount, null, null, null);
    }

    private void givenInvalidDatesUpdateRequest() {
        request = new UpdatePromotionRequest(
                promotionId,
                null,
                null,
                null,
                LocalDateTime.now().plusDays(10),
                LocalDateTime.now().plusDays(2)
        );
    }

    private void givenPromotionExists() {
        when(promotionRepository.findById(promotionId))
                .thenReturn(Optional.of(existingPromotion));
    }

    private void givenPromotionDoesNotExist() {
        when(promotionRepository.findById(promotionId)).thenReturn(Optional.empty());
    }

    private void givenPromotionCanBeSaved() {
        when(promotionRepository.save(any(Promotion.class)))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
    }

    private void whenUpdatingPromotion() {
        response = useCase.execute(request);
    }

    private void whenUpdatingPromotionFails() {
        thrownException = assertThrows(RuntimeException.class, this::whenUpdatingPromotion);
    }

    private void thenPromotionResponseWasReturned() {
        assertNotNull(response);
    }

    private void thenPromotionFieldsWereUpdated() {
        thenPromotionNameIs("New Name");
        assertEquals(new BigDecimal("25"), response.discountPercentage());
        assertEquals(request.startDate(), response.startDate());
        assertEquals(request.endDate(), response.endDate());
        thenPromotionStatusIs(PromotionStatus.ACTIVE);
    }

    private void thenPromotionProductsWereUpdated() {
        assertEquals(2, response.productIds().size());
        assertEquals(firstProductId, response.productIds().get(0));
        assertEquals(secondProductId, response.productIds().get(1));
    }

    private void thenPromotionNameIs(String name) {
        assertEquals(name, response.name());
    }

    private void thenDiscountIs(String discount) {
        assertEquals(new BigDecimal(discount), response.discountPercentage());
    }

    private void thenStartDateWasKept() {
        assertEquals(existingPromotion.getStartDate(), response.startDate());
    }

    private void thenPromotionStatusIs(PromotionStatus status) {
        assertEquals(status, response.status());
    }

    private void thenPromotionWasSaved() {
        verify(promotionRepository).save(any(Promotion.class));
    }

    private void thenPromotionWasNotFound() {
        assertTrue(thrownException instanceof IllegalArgumentException);
    }

    private void thenPromotionRequiresAtLeastTwoProducts() {
        assertEquals("A promotion requires at least 2 products.", thrownException.getMessage());
    }

    private void thenPromotionUpdateIsInvalid() {
        assertTrue(thrownException instanceof IllegalArgumentException);
    }

    private void thenPromotionWasNotSaved() {
        verify(promotionRepository, never()).save(any());
    }

    private Producto product(UUID productId) {
        Producto product = new Producto();
        product.setId(productId);
        return product;
    }
}
