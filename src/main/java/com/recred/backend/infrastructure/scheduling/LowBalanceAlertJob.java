package com.recred.backend.infrastructure.scheduling;

import com.recred.backend.application.GenerateLowBalanceAlerts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LowBalanceAlertJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(LowBalanceAlertJob.class);

    private final GenerateLowBalanceAlerts generateLowBalanceAlerts;

    public LowBalanceAlertJob(GenerateLowBalanceAlerts generateLowBalanceAlerts) {
        this.generateLowBalanceAlerts = generateLowBalanceAlerts;
    }

    @Scheduled(cron = "0 0 10 * * ?")
    public void executeLowBalanceAlerts() {
        LOGGER.info("Starting LowBalanceAlertJob execution");
        try {
            generateLowBalanceAlerts.execute();
            LOGGER.info("Successfully executed LowBalanceAlertJob");
        } catch (Exception e) {
            LOGGER.error("Error executing LowBalanceAlertJob: ", e);
        }
    }
}
