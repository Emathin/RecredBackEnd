package com.recred.backend.unit.application;

import com.recred.backend.application.GetActivePromotionsByBuffet;
import com.recred.backend.application.GetActivePromotionsByBuffetRequest;
import com.recred.backend.application.GetActivePromotionsByBuffetResponse;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.repositories.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetActivePromotionsByBuffetTest {

    @Mock
    private PromotionRepository promotionRepository;

    @InjectMocks
    private GetActivePromotionsByBuffet getActivePromotionsByBuffet;

    @Test
    void execute_returnsPromotionsForBuffet() {
        UUID buffetId = UUID.randomUUID();
        Promotion p1 = new Promotion();
        p1.setId(UUID.randomUUID());
        GetActivePromotionsByBuffetRequest request =
                new GetActivePromotionsByBuffetRequest(buffetId);
        
        when(promotionRepository.findAllActiveByBuffet(buffetId)).thenReturn(List.of(p1));

        GetActivePromotionsByBuffetResponse result = getActivePromotionsByBuffet
                .execute(request);

        assertEquals(1, result.promotions().size());
        assertEquals(p1.getId(), result.promotions().get(0).id());
    }
}
