package com.recred.backend.infrastructure.scheduling;

import com.recred.backend.application.ManagePromotionStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ManagePromotionStatusCheckJob {

    private final ManagePromotionStatus managePromotionStatus;

    public ManagePromotionStatusCheckJob(ManagePromotionStatus managePromotionStatus) {
        this.managePromotionStatus = managePromotionStatus;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void run() {
        managePromotionStatus.execute();
    }
}
