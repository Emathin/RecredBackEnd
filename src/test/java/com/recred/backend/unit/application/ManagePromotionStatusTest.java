package com.recred.backend.unit.application;

import com.recred.backend.application.ManagePromotionStatus;
import com.recred.backend.application.ManagePromotionStatusRequest;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.domain.repositories.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagePromotionStatusTest {

    private static final UUID PROMOTION_ID = UUID.randomUUID();

    @Mock
    private PromotionRepository promotionRepository;

    @InjectMocks
    private ManagePromotionStatus managePromotionStatus;

    @Test
    void execute_ShouldActivateDraftPromotions() {
        ManagePromotionStatusRequest request = givenRequest();
        givenDraftPromotion(PromotionStatus.INACTIVE);

        whenExecute(request);

        thenPromotionIsSavedWithStatus(PromotionStatus.ACTIVE);
    }

    @Test
    void execute_ShouldNotActivateAlreadyActivePromotions() {
        ManagePromotionStatusRequest request = givenRequest();
        givenDraftPromotion(PromotionStatus.ACTIVE);

        whenExecute(request);

        thenNoPromotionIsSaved();
    }



    private ManagePromotionStatusRequest givenRequest() {
        return new ManagePromotionStatusRequest();
    }

    private void givenDraftPromotion(PromotionStatus status) {
        Promotion draftPromotion = new Promotion();
        draftPromotion.setId(PROMOTION_ID);
        draftPromotion.setStatus(status);
        draftPromotion.setStartDate(LocalDateTime.now().minusDays(1));

        when(promotionRepository.findInactivePromotionsByStartDateBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(draftPromotion));
    }

    private void givenNoDraftPromotions() {
        when(promotionRepository.findInactivePromotionsByStartDateBefore(any(LocalDateTime.class)))
                .thenReturn(List.of());
    }



    private void whenExecute(ManagePromotionStatusRequest request) {
        managePromotionStatus.execute(request);
    }

    private void thenPromotionIsSavedWithStatus(PromotionStatus expectedStatus) {
        verify(promotionRepository).save(argThat(p -> 
                p.getId().equals(PROMOTION_ID) && p.getStatus() == expectedStatus
        ));
    }

    private void thenNoPromotionIsSaved() {
        verify(promotionRepository, never()).save(any(Promotion.class));
    }
}
