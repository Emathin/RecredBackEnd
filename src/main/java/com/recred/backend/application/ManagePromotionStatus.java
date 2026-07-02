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
        LocalDateTime today = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime tomorrow = today.plusDays(1);

        List<Promotion> promotionsToActivate =
                promotionRepository.findDraftPromotionsByStartDateBefore(tomorrow);

        for (Promotion promotion : promotionsToActivate) {
            if (promotion.getStatus() == PromotionStatus.DRAFT) {
                promotion.setStatus(PromotionStatus.ACTIVE);
                promotionRepository.save(promotion);
            }
        }
        return new ManagePromotionStatusResponse();
    }
}
