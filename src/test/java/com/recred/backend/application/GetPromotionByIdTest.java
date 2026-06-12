package com.recred.backend.application;

import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.repositories.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPromotionByIdTest {

    @Mock
    private PromotionRepository promotionRepository;

    @InjectMocks
    private GetPromotionById getPromotionById;

    @Test
    void execute_shouldReturnPromotion_whenPromotionExists() {
        UUID id = UUID.randomUUID();
        Promotion promotion = new Promotion();
        promotion.setId(id);

        when(promotionRepository.findById(id)).thenReturn(Optional.of(promotion));

        Promotion result = getPromotionById.execute(id);

        assertNotNull(result);
        assertEquals(id, result.getId());
        verify(promotionRepository).findById(id);
    }

    @Test
    void execute_shouldThrowException_whenPromotionDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(promotionRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> getPromotionById.execute(id));
        verify(promotionRepository).findById(id);
    }
}
