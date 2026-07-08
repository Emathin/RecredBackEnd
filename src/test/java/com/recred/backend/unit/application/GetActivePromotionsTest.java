package com.recred.backend.unit.application;

import com.recred.backend.application.GetActivePromotions;
import com.recred.backend.application.GetActivePromotionsRequest;
import com.recred.backend.application.GetActivePromotionsResponse;
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
class GetActivePromotionsTest {

    @Mock
    private PromotionRepository promotionRepository;

    private GetActivePromotions useCase;
    private GetActivePromotionsResponse response;
    private UUID firstPromotionId;
    private UUID secondPromotionId;

    @BeforeEach
    void setUp() {
        useCase = new GetActivePromotions(promotionRepository);
    }

    @Test
    void givenActivePromotionsExist_whenGettingPromotions_thenReturnsAllActivePromotions() {
        givenActivePromotionsExist();

        whenGetActivePromotions();

        thenActivePromotionsWereReturned();
        thenActivePromotionsWereRequested();
    }

    private void givenActivePromotionsExist() {
        firstPromotionId = randomUUID();
        secondPromotionId = randomUUID();

        when(promotionRepository.findAllActive()).thenReturn(List.of(
                promotion(firstPromotionId),
                promotion(secondPromotionId)
        ));
    }

    private void whenGetActivePromotions() {
        response = useCase.execute(new GetActivePromotionsRequest());
    }

    private void thenActivePromotionsWereReturned() {
        assertEquals(2, response.promotions().size());
        assertEquals(firstPromotionId, response.promotions().get(0).id());
        assertEquals(secondPromotionId, response.promotions().get(1).id());
    }

    private void thenActivePromotionsWereRequested() {
        verify(promotionRepository).findAllActive();
    }

    private Promotion promotion(UUID requestedPromotionId) {
        Promotion promotion = new Promotion();
        promotion.setId(requestedPromotionId);
        return promotion;
    }
}
