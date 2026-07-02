package com.recred.backend.unit.application;

import com.recred.backend.application.GetActivePromotions;
import com.recred.backend.application.GetActivePromotionsRequest;
import com.recred.backend.application.GetActivePromotionsResponse;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.repositories.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetActivePromotionsTest {

    @Mock
    private PromotionRepository promotionRepository;

    @InjectMocks
    private GetActivePromotions getActivePromotions;

    @Test
    void execute_shouldReturnAllActivePromotions() {
        Promotion p1 = new Promotion();
        Promotion p2 = new Promotion();
        List<Promotion> mockPromos = List.of(p1, p2);

        when(promotionRepository.findAllActive()).thenReturn(mockPromos);

        GetActivePromotionsResponse result = getActivePromotions.execute(
                new GetActivePromotionsRequest());

        assertNotNull(result);
        assertEquals(2, result.promotions().size());
        verify(promotionRepository).findAllActive();
    }
}
