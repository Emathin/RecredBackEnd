package com.recred.backend.unit.application;

import com.recred.backend.application.CreatePromotion;
import com.recred.backend.application.GenerateSeasonalRecommendation;
import com.recred.backend.infrastructure.gemini.GeminiSeasonalSuggester;
import com.recred.backend.infrastructure.openMeteo.OpenMeteoWeatherClient;
import com.recred.backend.infrastructure.web.RecommendationResponseDTO;
import com.recred.backend.infrastructure.web.SuggestedPromotionDTO;
import com.recred.backend.infrastructure.web.SugerenciaDTO;
import com.recred.backend.infrastructure.web.WeatherDTO;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.domain.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenerateSeasonalRecommendationTest {

    @Mock
    private OpenMeteoWeatherClient climaServiceMock;

    @Mock
    private GeminiSeasonalSuggester geminiSuggesterMock;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CreatePromotion createPromotion;

    @InjectMocks
    private GenerateSeasonalRecommendation useCase;

    @Test
    void dadoQueLosDosServiciosRespondenCorrectamente_cuandoSeEjecuta_entoncesRetornaRecomendaciones() {
        WeatherDTO climaSimulado = new WeatherDTO(25.0, "Despejado");
        when(climaServiceMock.getCurrentWeather(anyDouble(), anyDouble())).thenReturn(climaSimulado);

        RecommendationResponseDTO recomendacionSimulada = new RecommendationResponseDTO();
        recomendacionSimulada.setSugerences(List.of(
                new SugerenciaDTO("Helados y Congelados", "AUMENTAR", "Clima caluroso favorece ventas.")));
        recomendacionSimulada.setPromocionalTip("Promo 2x1 en helados.");
        when(geminiSuggesterMock.getSeasonalRecommendations(anyString(), anyDouble(), anyString()))
                .thenReturn(recomendacionSimulada);

        RecommendationResponseDTO resultado = useCase.execute(-34.7, -58.47, null);

        assertNotNull(resultado);
        assertEquals("Promo 2x1 en helados.", resultado.getPromocionalTip());
        assertEquals(1, resultado.getSugerences().size());
        assertEquals("Helados y Congelados", resultado.getSugerences().get(0).getCategoria());
    }

    @Test
    void dadoQueElServicioDeClimaFalla_cuandoSeEjecuta_entoncesLanzaRuntimeException() {
        when(climaServiceMock.getCurrentWeather(anyDouble(), anyDouble()))
                .thenThrow(new RuntimeException("Weather service is currently unavailable"));

        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> {
            useCase.execute(-34.7, -58.47, null);
        });

        assertEquals("Weather service is currently unavailable", excepcion.getMessage());
        verify(geminiSuggesterMock, never()).getSeasonalRecommendations(anyString(), anyDouble(), anyString());
    }

    @Test
    void dadoQueElServicioDeIaFalla_cuandoSeEjecuta_entoncesLanzaRuntimeException() {
        WeatherDTO climaSimulado = new WeatherDTO(5.0, "Frío Extremo");
        when(climaServiceMock.getCurrentWeather(anyDouble(), anyDouble())).thenReturn(climaSimulado);
        when(geminiSuggesterMock.getSeasonalRecommendations(anyString(), anyDouble(), anyString()))
                .thenThrow(new RuntimeException("Failed to generate seasonal recommendations from AI."));

        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> {
            useCase.execute(-34.7, -58.47, null);
        });

        assertEquals("Failed to generate seasonal recommendations from AI.", excepcion.getMessage());
    }

    @Test
    void dadoQueSeEjecuta_cuandoSeInvoca_entoncesLlamaAlClimaAntesQueLaIa() {
        WeatherDTO climaSimulado = new WeatherDTO(15.0, "Nublado");
        when(climaServiceMock.getCurrentWeather(anyDouble(), anyDouble())).thenReturn(climaSimulado);

        RecommendationResponseDTO recomendacionSimulada = new RecommendationResponseDTO();
        recomendacionSimulada.setSugerences(List.of());
        recomendacionSimulada.setPromocionalTip("Tip genérico.");
        when(geminiSuggesterMock.getSeasonalRecommendations(anyString(), anyDouble(), anyString()))
                .thenReturn(recomendacionSimulada);

        useCase.execute(10.0, 20.0, null);

        InOrder ordenDeInvocacion = inOrder(climaServiceMock, geminiSuggesterMock);
        ordenDeInvocacion.verify(climaServiceMock).getCurrentWeather(10.0, 20.0);
        ordenDeInvocacion.verify(geminiSuggesterMock)
                .getSeasonalRecommendations(anyString(), eq(15.0), eq("Nublado"));
    }

    @Test
    void dadoUnaPromocionSugerida_cuandoHayProductosDeCategoria_entoncesCreaPromocionDraft() {
        UUID productId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();
        WeatherDTO climaSimulado = new WeatherDTO(28.0, "Caluroso");
        when(climaServiceMock.getCurrentWeather(anyDouble(), anyDouble()))
                .thenReturn(climaSimulado);

        RecommendationResponseDTO recomendacion = new RecommendationResponseDTO();
        SuggestedPromotionDTO suggestedPromotion = new SuggestedPromotionDTO();
        suggestedPromotion.setName("Promo fresca");
        suggestedPromotion.setDiscountPercentage(new BigDecimal("15.00"));
        suggestedPromotion.setTargetCategories(List.of("bebida"));
        recomendacion.setSuggestedPromotion(suggestedPromotion);
        when(geminiSuggesterMock.getSeasonalRecommendations(anyString(), eq(28.0),
                eq("Caluroso"))).thenReturn(recomendacion);
        when(productRepository.findAllActive()).thenReturn(List.of(
                product(productId, "Bebidas frias"),
                product(secondProductId, "Bebidas frias")
        ));
        Promotion createdPromotion = promotion(UUID.randomUUID(), productId, secondProductId);
        when(createPromotion.execute(
                eq(List.of(productId, secondProductId)),
                eq(new BigDecimal("15.00")),
                eq("Promo fresca"),
                any(),
                any(),
                isNull(),
                eq(true)
        )).thenReturn(createdPromotion);

        RecommendationResponseDTO resultado = useCase.execute(-34.7, -58.47, null);

        assertNotNull(resultado.getCreatedPromotion());
        assertEquals(createdPromotion.getId(), resultado.getCreatedPromotion().getId());
        verify(createPromotion).execute(
                eq(List.of(productId, secondProductId)),
                eq(new BigDecimal("15.00")),
                eq("Promo fresca"),
                any(),
                any(),
                isNull(),
                eq(true)
        );
    }

    @Test
    void dadoUnaPromocionSugerida_cuandoHayMenosDeDosMatches_entoncesUsaPrimerosDosProductos() {
        UUID matchedProductId = UUID.randomUUID();
        UUID fallbackProductId = UUID.randomUUID();
        WeatherDTO climaSimulado = new WeatherDTO(10.0, "Frio");
        when(climaServiceMock.getCurrentWeather(anyDouble(), anyDouble()))
                .thenReturn(climaSimulado);

        RecommendationResponseDTO recomendacion = new RecommendationResponseDTO();
        SuggestedPromotionDTO suggestedPromotion = new SuggestedPromotionDTO();
        suggestedPromotion.setName("Promo invierno");
        suggestedPromotion.setDiscountPercentage(new BigDecimal("20.00"));
        suggestedPromotion.setTargetCategories(List.of("caliente"));
        recomendacion.setSuggestedPromotion(suggestedPromotion);
        when(geminiSuggesterMock.getSeasonalRecommendations(anyString(), eq(10.0),
                eq("Frio"))).thenReturn(recomendacion);
        when(productRepository.findAllActive()).thenReturn(List.of(
                product(matchedProductId, "Bebidas calientes"),
                product(fallbackProductId, "Snacks")
        ));
        Promotion createdPromotion = promotion(UUID.randomUUID(), matchedProductId,
                fallbackProductId);
        when(createPromotion.execute(
                eq(List.of(matchedProductId, fallbackProductId)),
                eq(new BigDecimal("20.00")),
                eq("Promo invierno"),
                any(),
                any(),
                isNull(),
                eq(true)
        )).thenReturn(createdPromotion);

        RecommendationResponseDTO resultado = useCase.execute(-34.7, -58.47, null);

        assertEquals(createdPromotion.getId(), resultado.getCreatedPromotion().getId());
        verify(createPromotion).execute(
                eq(List.of(matchedProductId, fallbackProductId)),
                eq(new BigDecimal("20.00")),
                eq("Promo invierno"),
                any(),
                any(),
                isNull(),
                eq(true)
        );
    }

    private Producto product(UUID productId, String categoryDescription) {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(UUID.randomUUID());
        category.setDescripcion(categoryDescription);

        Producto product = new Producto();
        product.setId(productId);
        product.setCategoria(category);
        return product;
    }

    private Promotion promotion(UUID promotionId, UUID firstProductId, UUID secondProductId) {
        Promotion promotion = new Promotion();
        promotion.setId(promotionId);
        promotion.setName("Promo");
        promotion.setDiscountPercentage(new BigDecimal("15.00"));
        promotion.setStatus(PromotionStatus.DRAFT);
        promotion.setProducts(List.of(product(firstProductId, "Bebidas"),
                product(secondProductId, "Snacks")));
        return promotion;
    }
}
