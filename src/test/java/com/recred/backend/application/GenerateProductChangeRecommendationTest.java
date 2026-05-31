package com.recred.backend.application;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

    @InjectMocks
    private GenerateProductChangeRecommendation useCase;

    @Test
    void executeShouldReturnAnalysisWhenProductExistsAndAiAnalyzes() {
        UUID sellerId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        ProductChangeRecommendation leastSold = new ProductChangeRecommendation();
        leastSold.setId(productId);
        leastSold.setIdBuffet(buffetId);
        leastSold.setNombre("Alfajor de Maicena");
        leastSold.setPrecio(new BigDecimal("1500.00"));

        ProductChangeAnalysis analysis = new ProductChangeAnalysis(
                "Resumen de prueba",
                List.of("Alerta 1"),
                List.of("Recomendacion 1"),
                "gemini-1.5-flash"
        );

        when(kiosqueroRepository.findBuffetById(sellerId)).thenReturn(buffetId);
        when(productRepository.findBuffetLeastSoldProducts(buffetId)).thenReturn(leastSold);
        when(productChangeAnalyzer.analyze(leastSold)).thenReturn(Optional.of(analysis));

        ProductRecommendationResult result = useCase.execute(sellerId);

        assertEquals(leastSold, result.getProductChangeRecommendation());
        assertEquals(analysis, result.getAiAnalysis().get());
        verify(aiEventRepository).registerProductChangeRecommendation(any(ProductRecommendationResult.class));
    }

    @Test
    void executeShouldThrowExceptionWhenKiosqueroNotFound() {
        UUID sellerId = UUID.randomUUID();
        when(kiosqueroRepository.findBuffetById(sellerId)).thenReturn(null);

        assertThrows(KiosqueroNotFoundException.class, () -> useCase.execute(sellerId));
        verify(productRepository, never()).findBuffetLeastSoldProducts(any());
    }

    @Test
    void executeAllShouldGenerateRecommendationsForAllKiosqueros() {
        UUID sellerId1 = UUID.randomUUID();
        UUID sellerId2 = UUID.randomUUID();
        UUID buffetId1 = UUID.randomUUID();
        UUID buffetId2 = UUID.randomUUID();

        when(kiosqueroRepository.findAllIds()).thenReturn(List.of(sellerId1, sellerId2));
        when(kiosqueroRepository.findBuffetById(sellerId1)).thenReturn(buffetId1);
        when(kiosqueroRepository.findBuffetById(sellerId2)).thenReturn(buffetId2);

        ProductChangeRecommendation leastSold1 = new ProductChangeRecommendation();
        leastSold1.setIdBuffet(buffetId1);
        ProductChangeRecommendation leastSold2 = new ProductChangeRecommendation();
        leastSold2.setIdBuffet(buffetId2);

        when(productRepository.findBuffetLeastSoldProducts(buffetId1)).thenReturn(leastSold1);
        when(productRepository.findBuffetLeastSoldProducts(buffetId2)).thenReturn(leastSold2);

        ProductChangeAnalysis analysis = new ProductChangeAnalysis(
                "Resumen", List.of(), List.of(), "model"
        );
        when(productChangeAnalyzer.analyze(any())).thenReturn(Optional.of(analysis));

        useCase.execute();

        verify(kiosqueroRepository).findAllIds();
        verify(kiosqueroRepository).findBuffetById(sellerId1);
        verify(kiosqueroRepository).findBuffetById(sellerId2);
        verify(aiEventRepository, org.mockito.Mockito.times(2)).registerProductChangeRecommendation(any());
    }
}
