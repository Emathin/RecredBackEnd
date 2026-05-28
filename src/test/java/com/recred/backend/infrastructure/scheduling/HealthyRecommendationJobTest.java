package com.recred.backend.infrastructure.scheduling;

import com.recred.backend.application.GenerateHealthyProductRecommendations;
import com.recred.backend.config.HealthyRecommendationProperties;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class HealthyRecommendationJobTest {

    @Test
    void runUseCaseWhenJobIsEnabled() {
        GenerateHealthyProductRecommendations useCase = mock(
                GenerateHealthyProductRecommendations.class);
        HealthyRecommendationJob job = new HealthyRecommendationJob(useCase, properties(true));

        job.run();

        verify(useCase).execute();
    }

    @Test
    void doNotRunUseCaseWhenJobIsDisabled() {
        GenerateHealthyProductRecommendations useCase = mock(
                GenerateHealthyProductRecommendations.class);
        HealthyRecommendationJob job = new HealthyRecommendationJob(useCase, properties(false));

        job.run();

        verify(useCase, never()).execute();
    }

    private HealthyRecommendationProperties properties(boolean enabled) {
        return new HealthyRecommendationProperties(
                enabled,
                "0 30 9 * * *",
                2,
                2,
                3,
                10,
                20
        );
    }
}
