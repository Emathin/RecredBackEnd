package com.recred.backend.unit.application;

import com.recred.backend.application.CreatePromotionResponse;
import com.recred.backend.application.GetPromotionById;
import com.recred.backend.application.GetPromotionByIdRequest;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.repositories.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPromotionByIdTest {

    @Mock
    private PromotionRepository promotionRepository;

    private GetPromotionById useCase;
    private UUID promotionId;

    @BeforeEach
    void setUp() {
        useCase = new GetPromotionById(promotionRepository);
    }

    @Test
    void givenPromotionExists_whenGettingPromotion_thenReturnsPromotion() {
        givenPromotionExists();

        final CreatePromotionResponse response = whenGetPromotionById();

        thenPromotionWasReturned(response);
        thenPromotionWasRequested();
    }

    @Test
    void givenPromotionDoesNotExist_whenGettingPromotion_thenThrowsException() {
        givenPromotionDoesNotExist();

        final IllegalArgumentException exception = whenGetPromotionByIdFails();

        thenPromotionNotFoundWasThrown(exception);
        thenPromotionWasRequested();
    }

    private void givenPromotionExists() {
        promotionId = randomUUID();
        when(promotionRepository.findById(promotionId))
                .thenReturn(Optional.of(promotion(promotionId)));
    }

    private void givenPromotionDoesNotExist() {
        promotionId = randomUUID();
        when(promotionRepository.findById(promotionId)).thenReturn(Optional.empty());
    }

    private CreatePromotionResponse whenGetPromotionById() {
        return useCase.execute(new GetPromotionByIdRequest(promotionId));
    }

    private IllegalArgumentException whenGetPromotionByIdFails() {
        return assertThrows(IllegalArgumentException.class, executeUseCase());
    }

    private void thenPromotionWasReturned(CreatePromotionResponse response) {
        assertEquals(promotionId, response.id());
    }

    private void thenPromotionNotFoundWasThrown(IllegalArgumentException exception) {
        assertEquals("Promotion not found.", exception.getMessage());
    }

    private void thenPromotionWasRequested() {
        verify(promotionRepository).findById(promotionId);
    }

    private Executable executeUseCase() {
        return () -> useCase.execute(new GetPromotionByIdRequest(promotionId));
    }

    private Promotion promotion(UUID requestedPromotionId) {
        Promotion promotion = new Promotion();
        promotion.setId(requestedPromotionId);
        return promotion;
    }
}
