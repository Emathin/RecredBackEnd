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

    @Mock
    private PromotionRepository promotionRepository;

    @InjectMocks
    private DeletePromotion deletePromotion;

    @Test
    void execute_shouldDeletePromotionBySettingStatusToRejected_whenPromotionExists() {
        UUID id = UUID.randomUUID();
        Promotion promotion = new Promotion();
        promotion.setId(id);
        promotion.setStatus(PromotionStatus.ACTIVE);

        when(promotionRepository.findById(id)).thenReturn(Optional.of(promotion));

        deletePromotion.execute(new DeletePromotionRequest(id));

        assertEquals(PromotionStatus.REJECTED, promotion.getStatus());
        verify(promotionRepository).save(promotion);
    }

    @Test
    void execute_shouldThrowException_whenPromotionDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(promotionRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> deletePromotion.execute(new DeletePromotionRequest(id)));
        verify(promotionRepository, never()).save(any());
    }
}
