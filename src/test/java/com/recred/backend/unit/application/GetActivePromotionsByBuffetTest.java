package com.recred.backend.unit.application;

import com.recred.backend.application.GetActivePromotionsByBuffet;
import com.recred.backend.application.GetActivePromotionsByBuffetRequest;
import com.recred.backend.application.GetActivePromotionsByBuffetResponse;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.repositories.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetActivePromotionsByBuffetTest {

    @Mock
    private PromotionRepository promotionRepository;

    private GetActivePromotionsByBuffet useCase;
    private GetActivePromotionsByBuffetResponse response;
    private UUID buffetId;
    private UUID promotionId;

    @BeforeEach
    void setUp() {
        useCase = new GetActivePromotionsByBuffet(promotionRepository);
    }

    @Test
    void givenBuffetHasActivePromotions_whenGettingPromotions_thenReturnsBuffetPromotions() {
        givenBuffetHasActivePromotion();

        whenGetActivePromotionsByBuffet();

        thenPromotionWasReturned();
        thenPromotionsWereRequestedForBuffet();
    }

    private void givenBuffetHasActivePromotion() {
        buffetId = randomUUID();
        promotionId = randomUUID();

        when(promotionRepository.findAllActiveByBuffet(buffetId))
                .thenReturn(List.of(promotion(promotionId)));
    }

    private void whenGetActivePromotionsByBuffet() {
        response = useCase.execute(new GetActivePromotionsByBuffetRequest(buffetId));
    }

    private void thenPromotionWasReturned() {
        assertEquals(1, response.promotions().size());
        assertEquals(promotionId, response.promotions().get(0).id());
    }

    private void thenPromotionsWereRequestedForBuffet() {
        verify(promotionRepository).findAllActiveByBuffet(buffetId);
    }

    private Promotion promotion(UUID requestedPromotionId) {
        Promotion promotion = new Promotion();
        promotion.setId(requestedPromotionId);
        return promotion;
    }
}
