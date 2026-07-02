package com.recred.backend.unit.infrastructure.scheduling;

import com.recred.backend.infrastructure.scheduling.ProductChangeRecomendationJob;

import com.recred.backend.application.GenerateProductChangeRecommendation;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProductChangeRecomendationJobTest {

    @Test
    void run_ShouldExecuteProductChangeRecommendationGeneration() {
        GenerateProductChangeRecommendation useCase = mock(GenerateProductChangeRecommendation.class);
        ProductChangeRecomendationJob job = new ProductChangeRecomendationJob(useCase);

        job.run();

        verify(useCase).execute();
    }
}
