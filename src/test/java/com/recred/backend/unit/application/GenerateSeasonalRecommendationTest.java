package com.recred.backend.unit.application;

import com.recred.backend.application.CreatePromotion;
import com.recred.backend.application.GenerateSeasonalRecommendation;
import com.recred.backend.infrastructure.gemini.GeminiSeasonalSuggester;
import com.recred.backend.infrastructure.openMeteo.NominatimGeocodingClient;
import com.recred.backend.infrastructure.openMeteo.OpenMeteoWeatherClient;
import com.recred.backend.infrastructure.web.RecommendationResponseDTO;
import com.recred.backend.infrastructure.web.SuggestedPromotionDTO;
import com.recred.backend.infrastructure.web.SugerenciaDTO;
import com.recred.backend.infrastructure.web.WeatherDTO;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.domain.repositories.CategoryRepository;
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
    private NominatimGeocodingClient geocodingClientMock;

    @Mock
    private GeminiSeasonalSuggester geminiSuggesterMock;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;


    @InjectMocks
    private GenerateSeasonalRecommendation useCase;

    @Test
    void dadoQueLosDosServiciosRespondenCorrectamente_cuandoSeEjecuta_entoncesRetornaRecomendaciones() {
        WeatherDTO climaSimulado = new WeatherDTO(25.0, "Despejado");
        when(climaServiceMock.getCurrentWeather(anyDouble(), anyDouble())).thenReturn(climaSimulado);

        RecommendationResponseDTO recomendacionSimulada = new RecommendationResponseDTO();
        recomendacionSimulada.setSugerences(List.of(
                new SugerenciaDTO("Helados y Congelados", "AUMENTAR", "Clima caluroso favorece ventas.", "🍦")));
        recomendacionSimulada.setPromocionalTip("Promo 2x1 en helados.");
        
        when(categoryRepository.getAllCategories()).thenReturn(List.of(
                product(UUID.randomUUID(), "Helados y Congelados").getCategoria()
        ));

        when(geminiSuggesterMock.getSeasonalRecommendations(anyString(), anyDouble(), anyString(), anyString()))
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
        verify(geminiSuggesterMock, never()).getSeasonalRecommendations(anyString(), anyDouble(), anyString(), anyString());
    }

    @Test
    void dadoQueElServicioDeIaFalla_cuandoSeEjecuta_entoncesLanzaRuntimeException() {
        WeatherDTO climaSimulado = new WeatherDTO(5.0, "Frío Extremo");
        when(climaServiceMock.getCurrentWeather(anyDouble(), anyDouble())).thenReturn(climaSimulado);
        
        when(categoryRepository.getAllCategories()).thenReturn(List.of());
        
        when(geminiSuggesterMock.getSeasonalRecommendations(anyString(), anyDouble(), anyString(), anyString()))
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
        when(categoryRepository.getAllCategories()).thenReturn(List.of());
        when(geminiSuggesterMock.getSeasonalRecommendations(anyString(), anyDouble(), anyString(), anyString()))
                .thenReturn(recomendacionSimulada);

        useCase.execute(10.0, 20.0, null);

        InOrder ordenDeInvocacion = inOrder(climaServiceMock, categoryRepository, geminiSuggesterMock);
        ordenDeInvocacion.verify(climaServiceMock).getCurrentWeather(10.0, 20.0);
        ordenDeInvocacion.verify(categoryRepository).getAllCategories();
        ordenDeInvocacion.verify(geminiSuggesterMock)
                .getSeasonalRecommendations(anyString(), eq(15.0), eq("Nublado"), anyString());
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
        when(categoryRepository.getAllCategories()).thenReturn(List.of());
        when(geminiSuggesterMock.getSeasonalRecommendations(anyString(), eq(28.0),
                eq("Caluroso"), anyString())).thenReturn(recomendacion);
        when(productRepository.findAllActiveWithStock(any())).thenReturn(List.of(
                product(productId, "Bebidas frias"),
                product(secondProductId, "Bebidas frias")
        ));
        RecommendationResponseDTO resultado = useCase.execute(-34.7, -58.47, null);

        assertNull(resultado.getCreatedPromotion());
        assertNotNull(resultado.getSuggestedPromotion().getProductIds());
        assertEquals(2, resultado.getSuggestedPromotion().getProductIds().size());
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
        when(categoryRepository.getAllCategories()).thenReturn(List.of());
        when(geminiSuggesterMock.getSeasonalRecommendations(anyString(), eq(10.0),
                eq("Frio"), anyString())).thenReturn(recomendacion);
        when(productRepository.findAllActiveWithStock(any())).thenReturn(List.of(
                product(matchedProductId, "Bebidas calientes"),
                product(fallbackProductId, "Snacks")
        ));
        RecommendationResponseDTO resultado = useCase.execute(-34.7, -58.47, null);

        assertNull(resultado.getCreatedPromotion());
        assertNotNull(resultado.getSuggestedPromotion().getProductIds());
        assertEquals(2, resultado.getSuggestedPromotion().getProductIds().size());
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
}
