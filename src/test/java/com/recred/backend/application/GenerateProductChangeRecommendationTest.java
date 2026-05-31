package com.recred.backend.application;

import com.recred.backend.domain.exceptions.MerchantNotFoundException;
import com.recred.backend.domain.models.ProductChangeAnalysis;
import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.ProductRecommendationResult;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.MerchantRepository;
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
    private MerchantRepository merchantRepository;

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
        leastSold.setName("Alfajor de Maicena");
        leastSold.setPrice(new BigDecimal("1500.00"));

        ProductChangeAnalysis analysis = new ProductChangeAnalysis(
                "Resumen de prueba",
                List.of("Alerta 1"),
                List.of("Recomendacion 1"),
                "gemini-1.5-flash"
        );

        when(merchantRepository.findBuffetById(sellerId)).thenReturn(buffetId);
        when(productRepository.findBuffetLeastSoldProducts(buffetId)).thenReturn(leastSold);
        when(productChangeAnalyzer.analyze(leastSold)).thenReturn(Optional.of(analysis));

        ProductChangeAnalysis result = useCase.execute(sellerId);

        assertEquals(analysis, result);
        verify(aiEventRepository).registerProductChangeRecommendation(any(ProductRecommendationResult.class));
    }

    @Test
    void executeShouldThrowExceptionWhenKiosqueroNotFound() {
        UUID sellerId = UUID.randomUUID();
        when(merchantRepository.findBuffetById(sellerId)).thenReturn(null);

        assertThrows(MerchantNotFoundException.class, () -> useCase.execute(sellerId));
        verify(productRepository, never()).findBuffetLeastSoldProducts(any());
    }

}
