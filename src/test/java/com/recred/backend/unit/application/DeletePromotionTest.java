package com.recred.backend.unit.application;

import com.recred.backend.application.DeletePromotion;
import com.recred.backend.application.DeletePromotionRequest;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.domain.repositories.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeletePromotionTest {

    private static final UUID PROMOTION_ID = UUID.randomUUID();

    @Mock
    private PromotionRepository promotionRepository;

    @InjectMocks
    private DeletePromotion useCase;

    @Test
    void rejectPromotionSuccessfully() {
        Promotion promotion = new Promotion();
        promotion.setId(PROMOTION_ID);
        promotion.setStatus(PromotionStatus.ACTIVE);
        
        givenAnExistingPromotion(promotion);

        whenExecuteUseCase();

        thenPromotionShouldBeDeleted(promotion);
    }

    @Test
    void throwExceptionWhenPromotionNotFound() {
        givenPromotionNotFound();

        assertThrows(IllegalArgumentException.class, this::whenExecuteUseCase);
        
        thenPromotionShouldNotBeDeleted();
    }

    private void givenAnExistingPromotion(Promotion promotion) {
        when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion));
    }

    private void givenPromotionNotFound() {
        when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.empty());
    }

    private void whenExecuteUseCase() {
        useCase.execute(new DeletePromotionRequest(PROMOTION_ID));
    }

    private void thenPromotionShouldBeDeleted(Promotion promotion) {
        verify(promotionRepository).deleteById(promotion.getId());
        verify(promotionRepository, never()).save(any());
    }

    private void thenPromotionShouldNotBeDeleted() {
        verify(promotionRepository, never()).deleteById(any());
    }
}
