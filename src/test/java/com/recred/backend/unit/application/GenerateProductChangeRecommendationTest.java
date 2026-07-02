package com.recred.backend.unit.application;

import com.recred.backend.application.GenerateProductChangeRecommendation;
import com.recred.backend.application.GenerateProductChangeRecommendationRequest;
import com.recred.backend.application.GetProductStatistics;
import com.recred.backend.application.ProductSaleStatisticsResponse;
import com.recred.backend.domain.exceptions.KiosqueroNotFoundException;
import com.recred.backend.domain.models.ProductChangeAnalysis;
import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.ProductRecommendationResult;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.services.ProductChangeAnalyzer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GenerateProductChangeRecommendationTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductChangeAnalyzer productChangeAnalyzer;

    @Mock
    private AIEventRepository aiEventRepository;

    @Mock
    private KiosqueroRepository kiosqueroRepository;

    @Mock
    private GetProductStatistics getProductStatistics;

    @InjectMocks
    private GenerateProductChangeRecommendation useCase;

    @Test
    void executeShouldGenerateRecommendationsForAllKiosqueros() {
        UUID sellerId1 = UUID.randomUUID();
        UUID sellerId2 = UUID.randomUUID();
        UUID buffetId1 = UUID.randomUUID();
        UUID buffetId2 = UUID.randomUUID();

        when(kiosqueroRepository.findAllIds()).thenReturn(List.of(sellerId1, sellerId2));
        when(kiosqueroRepository.findBuffetById(sellerId1)).thenReturn(buffetId1);
        when(kiosqueroRepository.findBuffetById(sellerId2)).thenReturn(buffetId2);
        when(kiosqueroRepository.findUsuarioIdById(sellerId1)).thenReturn(UUID.randomUUID());
        when(kiosqueroRepository.findUsuarioIdById(sellerId2)).thenReturn(UUID.randomUUID());

        ProductChangeRecommendation product1 = buildProduct(buffetId1);
        ProductChangeRecommendation product2 = buildProduct(buffetId2);

        when(productRepository.findBuffetTop5LeastSoldProducts(buffetId1))
                .thenReturn(List.of(product1));
        when(productRepository.findBuffetTop5LeastSoldProducts(buffetId2))
                .thenReturn(List.of(product2));

        ProductChangeAnalysis analysis = new ProductChangeAnalysis(
                "Resumen", List.of(), List.of(), "model"
        );
        when(getProductStatistics.execute(any())).thenReturn(statistics());
        when(productChangeAnalyzer.analyze(any())).thenReturn(Optional.of(analysis));

        useCase.execute(new GenerateProductChangeRecommendationRequest());

        verify(kiosqueroRepository).findAllIds();
        verify(kiosqueroRepository).findBuffetById(sellerId1);
        verify(kiosqueroRepository).findBuffetById(sellerId2);
        verify(aiEventRepository, times(2)).registerProductChangeRecommendation(any());
    }

    @Test
    void executeShouldContinueWhenOneKiosqueroFails() {
        UUID failingSellerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();

        when(kiosqueroRepository.findAllIds()).thenReturn(List.of(failingSellerId, sellerId));
        when(kiosqueroRepository.findBuffetById(failingSellerId)).thenReturn(null);
        when(kiosqueroRepository.findBuffetById(sellerId)).thenReturn(buffetId);
        when(kiosqueroRepository.findUsuarioIdById(sellerId)).thenReturn(UUID.randomUUID());

        ProductChangeRecommendation product = buildProduct(buffetId);
        when(productRepository.findBuffetTop5LeastSoldProducts(buffetId))
                .thenReturn(List.of(product));
        when(getProductStatistics.execute(any())).thenReturn(statistics());
        when(productChangeAnalyzer.analyze(product)).thenReturn(Optional.empty());

        useCase.execute(new GenerateProductChangeRecommendationRequest());

        verify(kiosqueroRepository).findBuffetById(failingSellerId);
        verify(kiosqueroRepository).findBuffetById(sellerId);
        verify(aiEventRepository).registerProductChangeRecommendation(
                any(ProductRecommendationResult.class));
    }

    @Test
    void executeShouldNotRegisterWhenNoKiosqueros() {
        when(kiosqueroRepository.findAllIds()).thenReturn(List.of());

        useCase.execute(new GenerateProductChangeRecommendationRequest());

        verify(productRepository, never()).findBuffetTop5LeastSoldProducts(any());
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
