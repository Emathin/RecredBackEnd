package com.recred.backend.unit.infrastructure.scheduling;

import com.recred.backend.application.AddProductRecommendation;
import com.recred.backend.application.AddProductRecommendationRequest;
import com.recred.backend.application.GeneratePurchaseRecommendation;
import com.recred.backend.application.GeneratePurchaseRecommendationRequest;
import com.recred.backend.infrastructure.scheduling.AddProductRecommendationJob;
import com.recred.backend.infrastructure.scheduling.AddPurchaseRecommendationJob;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RecommendationJobTest {

    private AddProductRecommendation addProductRecommendation;
    private AddProductRecommendationJob addProductRecommendationJob;
    private GeneratePurchaseRecommendation generatePurchaseRecommendation;
    private AddPurchaseRecommendationJob addPurchaseRecommendationJob;

    @Test
    void givenAddProductRecommendationJob_whenRunning_thenDelegatesToUseCase() {
        givenAddProductRecommendationJob();

        whenRunAddProductRecommendationJob();

        thenAddProductRecommendationWasCalled();
    }

    @Test
    void givenAddPurchaseRecommendationJob_whenRunning_thenDelegatesToUseCase() {
        givenAddPurchaseRecommendationJob();

        whenRunAddPurchaseRecommendationJob();

        thenAddPurchaseRecommendationWasCalled();
    }

    private void givenAddProductRecommendationJob() {
        addProductRecommendation = mock(AddProductRecommendation.class);
        addProductRecommendationJob = new AddProductRecommendationJob(addProductRecommendation);
    }

    private void givenAddPurchaseRecommendationJob() {
        generatePurchaseRecommendation = mock(GeneratePurchaseRecommendation.class);
        addPurchaseRecommendationJob =
                new AddPurchaseRecommendationJob(generatePurchaseRecommendation);
    }

    private void whenRunAddProductRecommendationJob() {
        addProductRecommendationJob.run();
    }

    private void whenRunAddPurchaseRecommendationJob() {
        addPurchaseRecommendationJob.run();
    }

    private void thenAddProductRecommendationWasCalled() {
        verify(addProductRecommendation).execute(new AddProductRecommendationRequest());
    }

    private void thenAddPurchaseRecommendationWasCalled() {
        verify(generatePurchaseRecommendation).execute(new GeneratePurchaseRecommendationRequest());
    }
}
