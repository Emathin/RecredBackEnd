package com.recred.backend.unit.application;

import com.recred.backend.application.GetProductChangeRecommendation;
import com.recred.backend.application.GetProductChangeRecommendationRequest;
import com.recred.backend.application.GetProductChangeRecommendationResponse;
import com.recred.backend.domain.exceptions.KiosqueroNotFoundException;
import com.recred.backend.domain.models.ProductChangeAnalysis;
import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.ProductRecommendationResult;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetProductChangeRecommendationTest {

    @Mock
    private KiosqueroRepository kiosqueroRepository;

    @Mock
    private AIEventRepository aiEventRepository;

    @InjectMocks
    private GetProductChangeRecommendation useCase;

    @Test
    void executeShouldReturnRecommendations() {
        UUID usuarioId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();

        ProductChangeRecommendation recommendation = new ProductChangeRecommendation();
        recommendation.setId(UUID.randomUUID());
        recommendation.setIdBuffet(buffetId);
        recommendation.setNombre("Galletitas");
        recommendation.setPrecio(BigDecimal.TEN);

        ProductChangeAnalysis analysis = new ProductChangeAnalysis(
                "Resumen", List.of(), List.of(), "model"
        );

        ProductRecommendationResult result = new ProductRecommendationResult(
                recommendation,
                Optional.of(analysis)
        );

        when(kiosqueroRepository.findBuffetByUsuarioId(usuarioId)).thenReturn(buffetId);
        when(aiEventRepository.findAllProductChangeRecommendations(buffetId))
                .thenReturn(List.of(result));

        GetProductChangeRecommendationRequest request =
                new GetProductChangeRecommendationRequest(usuarioId);

        List<GetProductChangeRecommendationResponse> responses = useCase.execute(request);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        GetProductChangeRecommendationResponse resp = responses.get(0);
        assertEquals(recommendation.getId(), resp.recommendationId());
        assertEquals("Galletitas", resp.originalProductName());
        assertEquals("Resumen", resp.summary());

        verify(kiosqueroRepository).findBuffetByUsuarioId(usuarioId);
        verify(aiEventRepository).findAllProductChangeRecommendations(buffetId);
    }

    @Test
    void executeShouldThrowExceptionWhenBuffetNotFound() {
        UUID usuarioId = UUID.randomUUID();
        when(kiosqueroRepository.findBuffetByUsuarioId(usuarioId)).thenReturn(null);

        GetProductChangeRecommendationRequest request =
                new GetProductChangeRecommendationRequest(usuarioId);

        assertThrows(KiosqueroNotFoundException.class, () -> useCase.execute(request));
    }
}
