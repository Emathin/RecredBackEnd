package com.recred.backend.unit.application;

import com.recred.backend.application.GetPromotionById;
import com.recred.backend.application.GetPromotionByIdRequest;
import com.recred.backend.application.CreatePromotionResponse;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.repositories.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
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
        GetPromotionByIdRequest request = new GetPromotionByIdRequest(id);

        when(promotionRepository.findById(id)).thenReturn(Optional.of(promotion));

        CreatePromotionResponse result = getPromotionById.execute(request);

        assertNotNull(result);
        assertEquals(id, result.id());
        verify(promotionRepository).findById(id);
    }

    @Test
    void execute_shouldThrowException_whenPromotionDoesNotExist() {
        UUID id = UUID.randomUUID();
        GetPromotionByIdRequest request = new GetPromotionByIdRequest(id);

        when(promotionRepository.findById(id)).thenReturn(Optional.empty());

        Executable execution = () -> getPromotionById.execute(request);

        assertThrows(IllegalArgumentException.class, execution);
        verify(promotionRepository).findById(id);
    }
}
