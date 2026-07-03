package com.recred.backend.unit.application;

import com.recred.backend.application.ManagePromotionStatus;
import com.recred.backend.application.ManagePromotionStatusRequest;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.domain.repositories.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagePromotionStatusTest {

    @Mock
    private PromotionRepository promotionRepository;

    private ManagePromotionStatus managePromotionStatus;

    @BeforeEach
    void setUp() {
        managePromotionStatus = new ManagePromotionStatus(promotionRepository);
        lenient().when(promotionRepository.findAllActive()).thenReturn(List.of());
    }

    @Test
    void execute_ShouldActivateDraftPromotions() {
        UUID promotionId = UUID.randomUUID();
        Promotion draftPromotion = new Promotion();
        draftPromotion.setId(promotionId);
        draftPromotion.setStatus(PromotionStatus.DRAFT);
        draftPromotion.setStartDate(LocalDateTime.now().minusDays(1));

        when(promotionRepository.findDraftPromotionsByStartDateBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(draftPromotion));

        managePromotionStatus.execute(new ManagePromotionStatusRequest());

        verify(promotionRepository)
                .save(argThat(p -> p.getId().equals(promotionId) && p.getStatus() == PromotionStatus.ACTIVE));
    }

    @Test
    void execute_ShouldNotActivateAlreadyActivePromotions() {
        Promotion activePromotion = new Promotion();
        activePromotion.setStatus(PromotionStatus.ACTIVE);

        when(promotionRepository.findDraftPromotionsByStartDateBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(activePromotion));

        managePromotionStatus.execute(new ManagePromotionStatusRequest());

        verify(promotionRepository, never()).save(any(Promotion.class));
    }

    @Test
    void execute_ShouldExpireActivePromotionsWhenEndDateHasPassed() {
        UUID promotionId = UUID.randomUUID();
        Promotion endedPromotion = new Promotion();
        endedPromotion.setId(promotionId);
        endedPromotion.setStatus(PromotionStatus.ACTIVE);
        endedPromotion.setEndDate(LocalDateTime.now().minusDays(1));

        when(promotionRepository.findDraftPromotionsByStartDateBefore(any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(promotionRepository.findAllActive()).thenReturn(List.of(endedPromotion));

        managePromotionStatus.execute(new ManagePromotionStatusRequest());

        verify(promotionRepository)
                .save(argThat(p -> p.getId().equals(promotionId) && p.getStatus() == PromotionStatus.EXPIRED));
    }
}
