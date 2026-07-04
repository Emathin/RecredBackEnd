package com.recred.backend.unit.application;

import com.recred.backend.application.GetProductChangeRecommendation;
import com.recred.backend.application.GetProductChangeRecommendationRequest;
import com.recred.backend.application.GetProductChangeRecommendationResponse;
import com.recred.backend.domain.exceptions.KiosqueroNotFoundException;
import com.recred.backend.domain.models.ProductChangeAnalysis;
import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.ProductRecommendationResult;
import com.recred.backend.domain.models.ProductSaleStatistics;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetProductChangeRecommendationTest {

    @Mock
    private KiosqueroRepository kiosqueroRepository;

    @Mock
    private AIEventRepository aiEventRepository;

    @Mock
    private com.recred.backend.domain.repositories.UsuarioRepository usuarioRepository;

    @InjectMocks
    private GetProductChangeRecommendation useCase;

    private final String cognitoSubject = "test-sub";

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

        com.recred.backend.domain.models.Usuario usuario = new com.recred.backend.domain.models.Usuario();
        usuario.setId(usuarioId);

        when(usuarioRepository.findByIdCognito(cognitoSubject)).thenReturn(Optional.of(usuario));
        when(kiosqueroRepository.findBuffetByUsuarioId(usuarioId)).thenReturn(buffetId);
        when(aiEventRepository.findAllProductChangeRecommendations(buffetId))
                .thenReturn(List.of(result));

        GetProductChangeRecommendationRequest request =
                new GetProductChangeRecommendationRequest(cognitoSubject);

        List<GetProductChangeRecommendationResponse> responses = useCase.execute(request);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        GetProductChangeRecommendationResponse resp = responses.get(0);
        assertEquals(recommendation.getId(), resp.recommendationId());
        assertEquals("Galletitas", resp.originalProductName());
        assertEquals("Resumen", resp.summary());

        verify(usuarioRepository).findByIdCognito(cognitoSubject);
        verify(kiosqueroRepository).findBuffetByUsuarioId(usuarioId);
        verify(aiEventRepository).findAllProductChangeRecommendations(buffetId);
    }

    @Test
    void executeShouldMapRecommendationsWithoutAnalysisAndWithSaleStatistics() {
        UUID usuarioId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        ProductSaleStatistics statistics = ProductSaleStatistics.builder()
                .productoId(productId)
                .nombre("Alfajor")
                .categoria("Snacks")
                .precioActual(new BigDecimal("120.50"))
                .ventasPeriodo(20L)
                .participacionVentas(new BigDecimal("0.15"))
                .rankingGeneral(3)
                .rankingCategoria(2)
                .promedioVentasCategoria(new BigDecimal("10.25"))
                .promedioPrecioCategoria(new BigDecimal("100.00"))
                .diferenciaPrecioCategoria(new BigDecimal("20.50"))
                .diasSinVenta(4L)
                .clientesDistintos(8L)
                .stockActual(30)
                .stockPromedioCategoria(new BigDecimal("25.75"))
                .build();

        ProductChangeRecommendation recommendation = new ProductChangeRecommendation();
        recommendation.setId(UUID.randomUUID());
        recommendation.setIdBuffet(buffetId);
        recommendation.setNombre("Alfajor");
        recommendation.setPrecio(new BigDecimal("120.50"));
        recommendation.setSaleStatistics(statistics);

        ProductRecommendationResult result = new ProductRecommendationResult(
                recommendation,
                Optional.empty()
        );
        com.recred.backend.domain.models.Usuario usuario =
                new com.recred.backend.domain.models.Usuario();
        usuario.setId(usuarioId);

        when(usuarioRepository.findByIdCognito(cognitoSubject)).thenReturn(Optional.of(usuario));
        when(kiosqueroRepository.findBuffetByUsuarioId(usuarioId)).thenReturn(buffetId);
        when(aiEventRepository.findAllProductChangeRecommendations(buffetId))
                .thenReturn(List.of(result));

        List<GetProductChangeRecommendationResponse> responses = useCase.execute(
                new GetProductChangeRecommendationRequest(cognitoSubject)
        );

        GetProductChangeRecommendationResponse response = responses.get(0);
        assertNull(response.summary());
        assertEquals(List.of(), response.alerts());
        assertEquals(List.of(), response.suggestedProducts());
        assertNull(response.model());

        GetProductChangeRecommendationResponse.ProductSaleStatisticsDto dto =
                response.saleStatistics();
        assertEquals(productId, dto.productoId());
        assertEquals("Alfajor", dto.nombre());
        assertEquals("Snacks", dto.categoria());
        assertEquals(new BigDecimal("120.50"), dto.precioActual());
        assertEquals(20L, dto.ventasPeriodo());
        assertEquals(new BigDecimal("0.15"), dto.participacionVentas());
        assertEquals(3, dto.rankingGeneral());
        assertEquals(2, dto.rankingCategoria());
        assertEquals(new BigDecimal("10.25"), dto.promedioVentasCategoria());
        assertEquals(new BigDecimal("100.00"), dto.promedioPrecioCategoria());
        assertEquals(new BigDecimal("20.50"), dto.diferenciaPrecioCategoria());
        assertEquals(4L, dto.diasSinVenta());
        assertEquals(8L, dto.clientesDistintos());
        assertEquals(30, dto.stockActual());
        assertEquals(new BigDecimal("25.75"), dto.stockPromedioCategoria());
    }

    @Test
    void executeShouldThrowExceptionWhenUserNotFound() {
        when(usuarioRepository.findByIdCognito(cognitoSubject)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> useCase.execute(new GetProductChangeRecommendationRequest(cognitoSubject))
        );

        assertEquals("User not found", exception.getMessage());
        verify(usuarioRepository).findByIdCognito(cognitoSubject);
        verifyNoInteractions(kiosqueroRepository, aiEventRepository);
    }

    @Test
    void executeShouldThrowExceptionWhenBuffetNotFound() {
        UUID usuarioId = UUID.randomUUID();
        com.recred.backend.domain.models.Usuario usuario = new com.recred.backend.domain.models.Usuario();
        usuario.setId(usuarioId);

        when(usuarioRepository.findByIdCognito(cognitoSubject)).thenReturn(Optional.of(usuario));
        when(kiosqueroRepository.findBuffetByUsuarioId(usuarioId)).thenReturn(null);

        GetProductChangeRecommendationRequest request =
                new GetProductChangeRecommendationRequest(cognitoSubject);

        assertThrows(KiosqueroNotFoundException.class, () -> useCase.execute(request));
    }
}
