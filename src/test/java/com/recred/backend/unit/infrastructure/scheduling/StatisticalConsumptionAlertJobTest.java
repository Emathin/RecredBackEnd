package com.recred.backend.unit.infrastructure.scheduling;

import com.recred.backend.application.GenerateStatisticalConsumptionAlerts;
import com.recred.backend.application.GenerateStatisticalConsumptionAlertsRequest;
import com.recred.backend.config.StatisticalConsumptionAlertProperties;
import com.recred.backend.infrastructure.scheduling.StatisticalConsumptionAlertJob;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class StatisticalConsumptionAlertJobTest {

    private GenerateStatisticalConsumptionAlerts useCase;
    private StatisticalConsumptionAlertJob job;

    @Test
    void givenJobIsEnabled_whenRunningJob_thenRunsUseCase() {
        givenJobIsEnabled();

        whenRunningJob();

        thenUseCaseWasExecuted();
    }

    @Test
    void givenJobIsDisabled_whenRunningJob_thenDoesNotRunUseCase() {
        givenJobIsDisabled();

        whenRunningJob();

        thenUseCaseWasNotExecuted();
    }

    private void givenJobIsEnabled() {
        givenJobWithEnabledProperty(true);
    }

    private void givenJobIsDisabled() {
        givenJobWithEnabledProperty(false);
    }

    private void whenRunningJob() {
        job.run();
    }

    private void thenUseCaseWasExecuted() {
        verify(useCase).execute(new GenerateStatisticalConsumptionAlertsRequest());
    }

    private void thenUseCaseWasNotExecuted() {
        verify(useCase, never()).execute(any());
    }

    private void givenJobWithEnabledProperty(boolean enabled) {
        useCase = mock(GenerateStatisticalConsumptionAlerts.class);
        job = new StatisticalConsumptionAlertJob(useCase, properties(enabled));
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

