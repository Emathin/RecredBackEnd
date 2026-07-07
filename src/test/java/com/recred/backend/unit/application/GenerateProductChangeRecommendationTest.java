package com.recred.backend.unit.application;

import com.recred.backend.application.GenerateProductChangeRecommendation;
import com.recred.backend.application.GenerateProductChangeRecommendationRequest;
import com.recred.backend.application.GetProductStatistics;
import com.recred.backend.application.ProductSaleStatisticsResponse;
import com.recred.backend.domain.models.ProductChangeAnalysis;
import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.ProductRecommendationResult;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.services.ProductChangeAnalyzer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenerateProductChangeRecommendationTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductChangeAnalyzer productChangeAnalyzer = mock(ProductChangeAnalyzer.class);
    private final AIEventRepository aiEventRepository = mock(AIEventRepository.class);
    private final KiosqueroRepository kiosqueroRepository = mock(KiosqueroRepository.class);
    private final GetProductStatistics getProductStatistics = mock(GetProductStatistics.class);

    private final GenerateProductChangeRecommendation useCase = new GenerateProductChangeRecommendation(
            productRepository,
            productChangeAnalyzer,
            aiEventRepository,
            kiosqueroRepository,
            getProductStatistics
    );

    @Test
    void generateRecommendationsForAllKiosqueros() {
        UUID sellerId1 = UUID.randomUUID();
        UUID sellerId2 = UUID.randomUUID();
        UUID buffetId1 = UUID.randomUUID();
        UUID buffetId2 = UUID.randomUUID();

        givenKiosquerosExist(sellerId1, sellerId2);
        givenBuffetForKiosquero(sellerId1, buffetId1);
        givenBuffetForKiosquero(sellerId2, buffetId2);
        givenUsuarioIdForKiosquero(sellerId1);
        givenUsuarioIdForKiosquero(sellerId2);

        givenLeastSoldProductsForBuffet(buffetId1);
        givenLeastSoldProductsForBuffet(buffetId2);
        givenProductStatisticsAreAvailable();
        givenAiAnalysisIsSuccessful();

        whenExecute();

        thenBuffetsAreSearched(sellerId1, sellerId2);
        thenRecommendationsAreRegistered(2);
    }

    @Test
    void continueWhenOneKiosqueroFails() {
        UUID failingSellerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();

        givenKiosquerosExist(failingSellerId, sellerId);
        givenBuffetForKiosquero(failingSellerId, null); // Simulates failure
        givenBuffetForKiosquero(sellerId, buffetId);
        givenUsuarioIdForKiosquero(sellerId);

        givenLeastSoldProductsForBuffet(buffetId);
        givenProductStatisticsAreAvailable();
        givenAiAnalysisFails();

        whenExecute();

        thenBuffetsAreSearched(failingSellerId, sellerId);
        thenRecommendationsAreRegistered(1);
    }

    @Test
    void doNotRegisterWhenNoKiosqueros() {
        givenNoKiosquerosExist();

        whenExecute();

        thenNoProductsAreSearched();
        thenNoRecommendationsAreRegistered();
    }

    private void givenKiosquerosExist(UUID... sellerIds) {
        when(kiosqueroRepository.findAllIds()).thenReturn(List.of(sellerIds));
    }

    private void givenNoKiosquerosExist() {
        when(kiosqueroRepository.findAllIds()).thenReturn(List.of());
    }

    private void givenBuffetForKiosquero(UUID sellerId, UUID buffetId) {
        when(kiosqueroRepository.findBuffetById(sellerId)).thenReturn(buffetId);
    }

    private void givenUsuarioIdForKiosquero(UUID sellerId) {
        when(kiosqueroRepository.findUsuarioIdById(sellerId)).thenReturn(UUID.randomUUID());
    }

    private void givenLeastSoldProductsForBuffet(UUID buffetId) {
        ProductChangeRecommendation product = buildProduct(buffetId);
        when(productRepository.findBuffetTop5LeastSoldProducts(buffetId))
                .thenReturn(List.of(product));
    }

    private void givenProductStatisticsAreAvailable() {
        when(getProductStatistics.execute(any())).thenReturn(statistics());
    }

    private void givenAiAnalysisIsSuccessful() {
        ProductChangeAnalysis analysis = new ProductChangeAnalysis(
                "Resumen", List.of(), List.of(), "model"
        );
        when(productChangeAnalyzer.analyze(any())).thenReturn(Optional.of(analysis));
    }

    private void givenAiAnalysisFails() {
        when(productChangeAnalyzer.analyze(any())).thenReturn(Optional.empty());
    }

    private void whenExecute() {
        useCase.execute(new GenerateProductChangeRecommendationRequest());
    }

    private void thenBuffetsAreSearched(UUID... sellerIds) {
        for (UUID sellerId : sellerIds) {
            verify(kiosqueroRepository).findBuffetById(sellerId);
        }
    }

    private void thenRecommendationsAreRegistered(int times) {
        verify(aiEventRepository, times(times)).registerProductChangeRecommendation(any(ProductRecommendationResult.class));
    }

    private void thenNoProductsAreSearched() {
        verify(productRepository, never()).findBuffetTop5LeastSoldProducts(any());
    }

    private void thenNoRecommendationsAreRegistered() {
        verify(aiEventRepository, never()).registerProductChangeRecommendation(any());
    }

    private ProductChangeRecommendation buildProduct(UUID buffetId) {
        ProductChangeRecommendation product = new ProductChangeRecommendation();
        product.setId(UUID.randomUUID());
        product.setIdBuffet(buffetId);
        product.setNombre("Alfajor");
        product.setPrecio(new BigDecimal("500.00"));
        return product;
    }

    private ProductSaleStatisticsResponse statistics() {
        return new ProductSaleStatisticsResponse(
                UUID.randomUUID(),
                "Alfajor",
                "Snacks",
                new BigDecimal("500.00"),
                1L,
                BigDecimal.ONE,
                1,
                1,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                0L,
                1L,
                10,
                BigDecimal.TEN
        );
    }
}
