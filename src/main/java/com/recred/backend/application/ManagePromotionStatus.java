package com.recred.backend.application;

import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.domain.repositories.PromotionRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class ManagePromotionStatus {

    private final PromotionRepository promotionRepository;

    public ManagePromotionStatus(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    @Transactional
    public ManagePromotionStatusResponse execute(ManagePromotionStatusRequest request) {
        activateDraftPromotions();
        expireActivePromotions();
        return new ManagePromotionStatusResponse();
    }

    private void activateDraftPromotions() {
        LocalDateTime tomorrow = LocalDateTime.now().with(LocalTime.MIN).plusDays(1);
        List<Promotion> promotionsToActivate =
                promotionRepository.findDraftPromotionsByStartDateBefore(tomorrow);

        for (Promotion promotion : promotionsToActivate) {
            if (promotion.getStatus() == PromotionStatus.DRAFT) {
                promotion.setStatus(PromotionStatus.ACTIVE);
                promotionRepository.save(promotion);
            }
        }
    }

    private void expireActivePromotions() {
        List<Promotion> activePromotions = promotionRepository.findAllActive();
        LocalDateTime now = LocalDateTime.now();
        for (Promotion promotion : activePromotions) {
            if (promotion.getEndDate() != null && now.isAfter(promotion.getEndDate())) {
                promotion.setStatus(PromotionStatus.EXPIRED);
                promotionRepository.save(promotion);
            }
        }
    }
}
