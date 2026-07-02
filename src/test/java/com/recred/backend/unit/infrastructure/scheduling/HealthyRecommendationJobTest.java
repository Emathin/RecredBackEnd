package com.recred.backend.unit.infrastructure.scheduling;

import com.recred.backend.application.GenerateHealthyProductRecommendations;
import com.recred.backend.application.GenerateHealthyProductRecommendationsRequest;
import com.recred.backend.config.HealthyRecommendationProperties;
import com.recred.backend.infrastructure.scheduling.HealthyRecommendationJob;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
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

        verify(useCase).execute(new GenerateHealthyProductRecommendationsRequest());
    }

    @Test
    void doNotRunUseCaseWhenJobIsDisabled() {
        GenerateHealthyProductRecommendations useCase = mock(
                GenerateHealthyProductRecommendations.class);
        HealthyRecommendationJob job = new HealthyRecommendationJob(useCase, properties(false));

        job.run();

        verify(useCase, never()).execute(any());
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

