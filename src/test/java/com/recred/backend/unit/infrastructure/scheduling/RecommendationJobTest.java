package com.recred.backend.unit.infrastructure.scheduling;

import com.recred.backend.infrastructure.scheduling.*;

import com.recred.backend.application.AddProductRecommendation;
import com.recred.backend.application.AddProductRecommendationRequest;
import com.recred.backend.application.GeneratePurchaseRecommendation;
import com.recred.backend.application.GeneratePurchaseRecommendationRequest;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RecommendationJobTest {

    @Test
    void addProductRecommendationJobShouldDelegateToUseCase() {
        AddProductRecommendation useCase = mock(AddProductRecommendation.class);
        AddProductRecommendationJob job = new AddProductRecommendationJob(useCase);

        job.run();

        verify(useCase).execute(new AddProductRecommendationRequest());
    }

    @Test
    void addPurchaseRecommendationJobShouldDelegateToUseCase() {
        GeneratePurchaseRecommendation useCase = mock(GeneratePurchaseRecommendation.class);
        AddPurchaseRecommendationJob job = new AddPurchaseRecommendationJob(useCase);

        job.run();

        verify(useCase).execute(new GeneratePurchaseRecommendationRequest());
    }
}
