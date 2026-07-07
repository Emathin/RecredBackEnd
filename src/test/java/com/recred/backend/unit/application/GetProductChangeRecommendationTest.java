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
import org.junit.jupiter.api.function.Executable;
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
class GetProductChangeRecommendationTest {

    private static final String COGNITO_SUBJECT = "test-sub";
    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID BUFFET_ID = UUID.randomUUID();
    private static final UUID RECOMMENDATION_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @Mock
    private KiosqueroRepository kiosqueroRepository;

    @Mock
    private AIEventRepository aiEventRepository;

    @Mock
    private com.recred.backend.domain.repositories.UsuarioRepository usuarioRepository;

    @InjectMocks
    private GetProductChangeRecommendation useCase;

    @Test
    void executeShouldReturnRecommendations() {
        ProductRecommendationResult result = buildRecommendationResultWithAnalysis();
        givenUsuarioFound();
        givenBuffetFound();
        givenRecommendationsFound(List.of(result));

        List<GetProductChangeRecommendationResponse> responses = whenExecute();

        thenRecommendationsAreReturnedCorrectly(responses);
        thenRepositoriesWereQueried();
    }

    @Test
    void executeShouldMapRecommendationsWithoutAnalysisAndWithSaleStatistics() {
        ProductRecommendationResult result = buildRecommendationResultWithoutAnalysisAndWithStats();
        givenUsuarioFound();
        givenBuffetFound();
        givenRecommendationsFound(List.of(result));

        List<GetProductChangeRecommendationResponse> responses = whenExecute();

        thenStatisticsAreMappedCorrectly(responses);
    }

    @Test
    void executeShouldThrowExceptionWhenUserNotFound() {
        givenUsuarioNotFound();

        Executable execution = () -> whenExecute();
        thenThrowsUserNotFoundException(execution);
    }

    @Test
    void executeShouldThrowExceptionWhenBuffetNotFound() {
        givenUsuarioFound();
        givenBuffetNotFound();

        Executable execution = () -> whenExecute();
        thenThrowsKiosqueroNotFoundException(execution);
    }

    private ProductRecommendationResult buildRecommendationResultWithAnalysis() {
        ProductChangeRecommendation recommendation = new ProductChangeRecommendation();
        recommendation.setId(RECOMMENDATION_ID);
        recommendation.setIdBuffet(BUFFET_ID);
        recommendation.setNombre("Galletitas");
        recommendation.setPrecio(BigDecimal.TEN);

        ProductChangeAnalysis analysis = new ProductChangeAnalysis("Resumen", List.of(), List.of(), "model");

        return new ProductRecommendationResult(recommendation, Optional.of(analysis));
    }

    private ProductRecommendationResult buildRecommendationResultWithoutAnalysisAndWithStats() {
        ProductSaleStatistics statistics = ProductSaleStatistics.builder()
                .productoId(PRODUCT_ID)
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
        recommendation.setId(RECOMMENDATION_ID);
        recommendation.setIdBuffet(BUFFET_ID);
        recommendation.setNombre("Alfajor");
        recommendation.setPrecio(new BigDecimal("120.50"));
        recommendation.setSaleStatistics(statistics);

        return new ProductRecommendationResult(recommendation, Optional.empty());
    }

    private void givenUsuarioFound() {
        com.recred.backend.domain.models.Usuario usuario = new com.recred.backend.domain.models.Usuario();
        usuario.setId(USUARIO_ID);
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(usuario));
    }

    private void givenUsuarioNotFound() {
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.empty());
    }

    private void givenBuffetFound() {
        when(kiosqueroRepository.findBuffetByUsuarioId(USUARIO_ID)).thenReturn(BUFFET_ID);
    }

    private void givenBuffetNotFound() {
        when(kiosqueroRepository.findBuffetByUsuarioId(USUARIO_ID)).thenReturn(null);
    }

    private void givenRecommendationsFound(List<ProductRecommendationResult> recommendations) {
        when(aiEventRepository.findAllProductChangeRecommendations(BUFFET_ID)).thenReturn(recommendations);
    }

    private List<GetProductChangeRecommendationResponse> whenExecute() {
        return useCase.execute(new GetProductChangeRecommendationRequest(COGNITO_SUBJECT));
    }

    private void thenRecommendationsAreReturnedCorrectly(List<GetProductChangeRecommendationResponse> responses) {
        assertNotNull(responses);
        assertEquals(1, responses.size());
        GetProductChangeRecommendationResponse resp = responses.get(0);
        assertEquals(RECOMMENDATION_ID, resp.recommendationId());
        assertEquals("Galletitas", resp.originalProductName());
        assertEquals("Resumen", resp.summary());
    }

    private void thenRepositoriesWereQueried() {
        verify(usuarioRepository).findByIdCognito(COGNITO_SUBJECT);
        verify(kiosqueroRepository).findBuffetByUsuarioId(USUARIO_ID);
        verify(aiEventRepository).findAllProductChangeRecommendations(BUFFET_ID);
    }

    private void thenStatisticsAreMappedCorrectly(List<GetProductChangeRecommendationResponse> responses) {
        GetProductChangeRecommendationResponse response = responses.get(0);
        assertNull(response.summary());
        assertEquals(List.of(), response.alerts());
        assertEquals(List.of(), response.suggestedProducts());
        assertNull(response.model());

        GetProductChangeRecommendationResponse.ProductSaleStatisticsDto dto = response.saleStatistics();
        assertEquals(PRODUCT_ID, dto.productoId());
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

    private void thenThrowsUserNotFoundException(Executable execution) {
        RuntimeException exception = assertThrows(RuntimeException.class, execution);
        assertEquals("User not found", exception.getMessage());
        verify(usuarioRepository).findByIdCognito(COGNITO_SUBJECT);
        verifyNoInteractions(kiosqueroRepository, aiEventRepository);
    }

    private void thenThrowsKiosqueroNotFoundException(Executable execution) {
        assertThrows(KiosqueroNotFoundException.class, execution);
    }
}
