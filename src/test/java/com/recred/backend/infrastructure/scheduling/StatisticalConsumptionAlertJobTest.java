package com.recred.backend.infrastructure.scheduling;

import com.recred.backend.application.GenerateStatisticalConsumptionAlerts;
import com.recred.backend.config.StatisticalConsumptionAlertProperties;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class StatisticalConsumptionAlertJobTest {

    @Test
    void runUseCaseWhenJobIsEnabled() {
        GenerateStatisticalConsumptionAlerts useCase = mock(
                GenerateStatisticalConsumptionAlerts.class);
        StatisticalConsumptionAlertJob job = new StatisticalConsumptionAlertJob(
                useCase,
                properties(true)
        );

        job.run();

        verify(useCase).execute();
    }

    @Test
    void doNotRunUseCaseWhenJobIsDisabled() {
        GenerateStatisticalConsumptionAlerts useCase = mock(
                GenerateStatisticalConsumptionAlerts.class);
        StatisticalConsumptionAlertJob job = new StatisticalConsumptionAlertJob(
                useCase,
                properties(false)
        );

        job.run();

        verify(useCase, never()).execute();
    }

    private StatisticalConsumptionAlertProperties properties(boolean enabled) {
        return new StatisticalConsumptionAlertProperties(
                enabled,
                "0 0 9 * * *",
                30,
                3,
                7,
                14,
                2L
        );
    }
}
