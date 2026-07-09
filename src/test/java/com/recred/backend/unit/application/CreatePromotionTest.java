package com.recred.backend.unit.application;

import com.recred.backend.application.CreatePromotion;
import com.recred.backend.application.CreatePromotionRequest;
import com.recred.backend.application.CreatePromotionResponse;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.domain.repositories.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-02T12:00:00Z"),
            ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
    private static final String PROMOTION_NAME = "Promo Test";
    private static final BigDecimal DISCOUNT = new BigDecimal("20");
    private static final String IMAGE_URL =
            "https://res.cloudinary.com/djzfudbze/image/upload/v1/collage.png";

    @Mock
    private PromotionRepository promotionRepository;
    @Captor
    private ArgumentCaptor<Promotion> promotionCaptor;

    private CreatePromotion useCase;
    private List<UUID> productIds;
    private UUID buffetId;
    private CreatePromotionResponse response;
    private Throwable thrownException;

    @BeforeEach
    void setUp() {
        useCase = new CreatePromotion(promotionRepository, CLOCK);
        productIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        buffetId = UUID.randomUUID();
    }

    @Test
    void forcedDraftPromotionIsPersistedAsDraft() {
        givenPromotionCanBeSaved();

        whenCreatingPromotion(activePromotionRequest(true));

        thenResponseHasStatus(PromotionStatus.INACTIVE);
        thenPromotionWasPersistedWithStatus(PromotionStatus.INACTIVE);
        thenResponseContainsPromotionData();
    }

    @Test
    void currentPromotionIsPersistedAsActiveWhenDraftIsNotForced() {
        givenPromotionCanBeSaved();

        whenCreatingPromotion(activePromotionRequest(false));

        thenResponseHasStatus(PromotionStatus.ACTIVE);
        thenPromotionWasPersistedWithStatus(PromotionStatus.ACTIVE);
    }

    @Test
    void futurePromotionIsPersistedAsDraft() {
        givenPromotionCanBeSaved();

        whenCreatingPromotion(futurePromotionRequest());

        thenResponseHasStatus(PromotionStatus.INACTIVE);
        thenPromotionWasPersistedWithStatus(PromotionStatus.INACTIVE);
    }

    @Test
    void promotionCanBeCreatedWithBuffetAndImage() {
        givenPromotionCanBeSaved();

        whenCreatingPromotion(requestWithBuffetAndImage());

        thenPromotionWasPersistedWithBuffetAndImage();
        thenResponseContainsBuffetAndImage();
    }

    @Test
    void promotionWithLessThanTwoProductsIsRejected() {
        whenCreatingPromotionFails(
                requestWithProductIds(List.of(UUID.randomUUID())),
                IllegalArgumentException.class);

        thenErrorMessageIs("A promotion requires at least 2 products.");
        thenPromotionWasNotSaved();
    }

    @Test
    void zeroDiscountIsRejected() {
        whenCreatingPromotionFails(
                requestWithDiscount(BigDecimal.ZERO),
                IllegalArgumentException.class);

        thenErrorMessageIs("Discount percentage must be between 0 and 100.");
        thenPromotionWasNotSaved();
    }

    @Test
    void negativeDiscountIsRejected() {
        whenCreatingPromotionFails(
                requestWithDiscount(new BigDecimal("-5")),
                IllegalArgumentException.class);

        thenErrorMessageIs("Discount percentage must be between 0 and 100.");
        thenPromotionWasNotSaved();
    }

    @Test
    void discountGreaterThanOneHundredIsRejected() {
        whenCreatingPromotionFails(
                requestWithDiscount(new BigDecimal("101")),
                IllegalArgumentException.class);

        thenErrorMessageIs("Discount percentage must be between 0 and 100.");
        thenPromotionWasNotSaved();
    }

    @Test
    void startDateAfterEndDateIsRejected() {
        whenCreatingPromotionFails(
                requestWithDates(NOW.plusDays(5), NOW.plusDays(2)),
                IllegalArgumentException.class);

        thenErrorMessageIs("Invalid date range.");
        thenPromotionWasNotSaved();
    }

    private void givenPromotionCanBeSaved() {
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(invocation ->
                invocation.getArgument(0));
    }

    private void whenCreatingPromotion(CreatePromotionRequest request) {
        response = useCase.execute(request);
    }

    private <T extends Throwable> void whenCreatingPromotionFails(
            CreatePromotionRequest request,
            Class<T> exceptionType) {
        Executable execution = () -> useCase.execute(request);
        thrownException = assertThrows(exceptionType, execution);
    }

    private void thenResponseHasStatus(PromotionStatus expectedStatus) {
        assertNotNull(response);
        assertEquals(expectedStatus, response.status());
    }

    private void thenPromotionWasPersistedWithStatus(PromotionStatus expectedStatus) {
        verify(promotionRepository).save(promotionCaptor.capture());
        assertEquals(expectedStatus, promotionCaptor.getValue().getStatus());
    }

    private void thenResponseContainsPromotionData() {
        assertEquals(PROMOTION_NAME, response.name());
        assertEquals(DISCOUNT, response.discountPercentage());
        assertEquals(activeStartDate(), response.startDate());
        assertEquals(activeEndDate(), response.endDate());
        assertEquals(productIds, response.productIds());
    }

    private void thenPromotionWasPersistedWithBuffetAndImage() {
        verify(promotionRepository).save(promotionCaptor.capture());
        Promotion savedPromotion = promotionCaptor.getValue();
        assertEquals(buffetId, savedPromotion.getBuffet().getId());
        assertEquals(IMAGE_URL, savedPromotion.getImageUrl());
    }

    private void thenResponseContainsBuffetAndImage() {
        assertEquals(buffetId, response.buffetId());
        assertEquals(IMAGE_URL, response.imageUrl());
    }

    private void thenPromotionWasNotSaved() {
        verify(promotionRepository, never()).save(any(Promotion.class));
    }

    private void thenErrorMessageIs(String expectedMessage) {
        assertEquals(expectedMessage, thrownException.getMessage());
    }

    private CreatePromotionRequest activePromotionRequest(boolean forceDraft) {
        return request(
                productIds,
                DISCOUNT,
                PROMOTION_NAME,
                activeStartDate(),
                activeEndDate(),
                null,
                forceDraft,
                null
        );
    }

    private CreatePromotionRequest futurePromotionRequest() {
        return request(
                productIds,
                DISCOUNT,
                "Promo Future",
                NOW.plusDays(5),
                NOW.plusDays(10),
                null,
                false,
                null
        );
    }

    private CreatePromotionRequest requestWithBuffetAndImage() {
        return request(
                productIds,
                DISCOUNT,
                PROMOTION_NAME,
                activeStartDate(),
                activeEndDate(),
                buffetId,
                false,
                IMAGE_URL
        );
    }

    private CreatePromotionRequest requestWithProductIds(List<UUID> requestedProductIds) {
        return request(
                requestedProductIds,
                DISCOUNT,
                PROMOTION_NAME,
                activeStartDate(),
                activeEndDate(),
                null,
                false,
                null
        );
    }

    private CreatePromotionRequest requestWithDiscount(BigDecimal discount) {
        return request(
                productIds,
                discount,
                PROMOTION_NAME,
                activeStartDate(),
                activeEndDate(),
                null,
                false,
                null
        );
    }

    private CreatePromotionRequest requestWithDates(
            LocalDateTime startDate,
            LocalDateTime endDate) {
        return request(
                productIds,
                DISCOUNT,
                PROMOTION_NAME,
                startDate,
                endDate,
                null,
                false,
                null
        );
    }

    private CreatePromotionRequest request(
            List<UUID> requestedProductIds,
            BigDecimal discount,
            String name,
            LocalDateTime startDate,
            LocalDateTime endDate,
            UUID requestedBuffetId,
            boolean forceDraft,
            String imageUrl
    ) {
        return new CreatePromotionRequest(
                requestedProductIds,
                discount,
                name,
                startDate,
                endDate,
                requestedBuffetId,
                forceDraft,
                imageUrl
        );
    }

    private LocalDateTime activeStartDate() {
        return NOW.minusDays(1);
    }

    private LocalDateTime activeEndDate() {
        return NOW.plusDays(5);
    }
}
