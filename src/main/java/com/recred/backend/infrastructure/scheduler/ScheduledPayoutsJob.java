package com.recred.backend.infrastructure.scheduler;

import com.recred.backend.application.ProcessScheduledPayouts;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledPayoutsJob {

    private final ProcessScheduledPayouts processScheduledPayouts;

    public ScheduledPayoutsJob(ProcessScheduledPayouts processScheduledPayouts) {
        this.processScheduledPayouts = processScheduledPayouts;
    }

    @Scheduled(cron = "${app.scheduler.payouts-cron:0 0 1 * * ?}")
    public void executePayouts() {
        processScheduledPayouts.execute();
    }
}
