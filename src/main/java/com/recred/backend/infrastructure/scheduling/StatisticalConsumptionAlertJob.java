package com.recred.backend.infrastructure.scheduling;

import com.recred.backend.application.GenerateStatisticalConsumptionAlerts;
import com.recred.backend.config.StatisticalConsumptionAlertProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StatisticalConsumptionAlertJob {

    private final GenerateStatisticalConsumptionAlerts generateStatisticalConsumptionAlerts;
    private final StatisticalConsumptionAlertProperties properties;

    public StatisticalConsumptionAlertJob(
            GenerateStatisticalConsumptionAlerts generateStatisticalConsumptionAlerts,
            StatisticalConsumptionAlertProperties properties
    ) {
        this.generateStatisticalConsumptionAlerts = generateStatisticalConsumptionAlerts;
        this.properties = properties;
    }

    @Scheduled(cron = "${statistical-alert.cron:0 0 9 * * *}")
    public void run() {
        if (!properties.enabled()) {
            return;
        }
        generateStatisticalConsumptionAlerts.execute();
    }
}
