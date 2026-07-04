package com.recred.backend.unit.application;

import com.recred.backend.application.CreatePromotion;
import com.recred.backend.application.GenerateSeasonalRecommendation;
import com.recred.backend.application.GenerateSeasonalRecommendationRequest;
import com.recred.backend.application.SeasonalRecommendationResponse;
import com.recred.backend.infrastructure.gemini.GeminiSeasonalSuggester;
import com.recred.backend.infrastructure.openMeteo.NominatimGeocodingClient;
import com.recred.backend.infrastructure.openMeteo.OpenMeteoWeatherClient;
import com.recred.backend.infrastructure.web.GeocodingResponseDTO;
import com.recred.backend.infrastructure.web.PromotionResponseDTO;
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
import java.time.LocalDateTime;
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

        SeasonalRecommendationResponse resultado = execute(-34.7, -58.47, null);

        assertNotNull(resultado);
        assertEquals("Promo 2x1 en helados.", resultado.promotionalTip());
        assertEquals(1, resultado.suggestions().size());
        assertEquals("Helados y Congelados", resultado.suggestions().get(0).category());
    }

    @Test
    void dadoQueElServicioDeClimaFalla_cuandoSeEjecuta_entoncesLanzaRuntimeException() {
        when(climaServiceMock.getCurrentWeather(anyDouble(), anyDouble()))
                .thenThrow(new RuntimeException("Weather service is currently unavailable"));

        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> {
            execute(-34.7, -58.47, null);
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
            execute(-34.7, -58.47, null);
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

        execute(10.0, 20.0, null);

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
        SeasonalRecommendationResponse resultado = execute(-34.7, -58.47, null);

        assertNull(resultado.createdPromotion());
        assertNotNull(resultado.suggestedPromotion().productIds());
        assertEquals(2, resultado.suggestedPromotion().productIds().size());
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
        SeasonalRecommendationResponse resultado = execute(-34.7, -58.47, null);

        assertNull(resultado.createdPromotion());
        assertNotNull(resultado.suggestedPromotion().productIds());
        assertEquals(2, resultado.suggestedPromotion().productIds().size());
    }

    @Test
    void dadoGeocodingYPromocionCreada_cuandoSeEjecuta_entoncesMapeaUbicacionYPromocion() {
        UUID buffetId = UUID.randomUUID();
        UUID promotionId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        WeatherDTO climaSimulado = new WeatherDTO(12.5, "Frio");
        when(climaServiceMock.getCurrentWeather(anyDouble(), anyDouble()))
                .thenReturn(climaSimulado);
        when(geocodingClientMock.getReverseGeocoding(anyDouble(), anyDouble()))
                .thenReturn(geocoding("Tandil", "Buenos Aires"));

        RecommendationResponseDTO recomendacion = new RecommendationResponseDTO();
        recomendacion.setPromocionalTip("Sumar productos calientes.");
        SuggestedPromotionDTO suggestedPromotion = new SuggestedPromotionDTO();
        suggestedPromotion.setName("Combo caliente");
        suggestedPromotion.setDiscountPercentage(new BigDecimal("10.00"));
        suggestedPromotion.setTargetCategories(List.of());
        suggestedPromotion.setImagen("combo.png");
        recomendacion.setSuggestedPromotion(suggestedPromotion);
        recomendacion.setCreatedPromotion(createdPromotion(promotionId, productId, buffetId));

        CategoriaProducto activeCategory = category("Infusiones", true);
        CategoriaProducto inactiveCategory = category("Oculta", false);
        when(categoryRepository.getAllCategories()).thenReturn(List.of(
                activeCategory,
                inactiveCategory
        ));
        when(geminiSuggesterMock.getSeasonalRecommendations(
                anyString(),
                eq(12.5),
                eq("Frio"),
                eq("Infusiones")))
                .thenReturn(recomendacion);
        when(productRepository.findAllActiveWithStock(buffetId))
                .thenReturn(List.of(product(productId, "Infusiones")));

        SeasonalRecommendationResponse resultado = execute(-37.32, -59.13, buffetId);

        assertNull(resultado.suggestions());
        assertEquals("Tandil", resultado.currentWeather().city());
        assertEquals("Buenos Aires", resultado.currentWeather().province());
        assertEquals("Combo caliente", resultado.suggestedPromotion().name());
        assertEquals(List.of(), resultado.suggestedPromotion().targetCategories());
        assertNull(resultado.suggestedPromotion().productIds());
        assertEquals(promotionId, resultado.createdPromotion().id());
        assertEquals(productId, resultado.createdPromotion().productIds().get(0));
        assertEquals(buffetId, resultado.createdPromotion().buffetId());
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

    private CategoriaProducto category(String description, boolean active) {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(UUID.randomUUID());
        category.setDescripcion(description);
        category.setActivo(active);
        return category;
    }

    private GeocodingResponseDTO geocoding(String town, String state) {
        GeocodingResponseDTO geocoding = new GeocodingResponseDTO();
        GeocodingResponseDTO.Address address = new GeocodingResponseDTO.Address();
        address.setTown(town);
        address.setState(state);
        geocoding.setAddress(address);
        return geocoding;
    }

    private PromotionResponseDTO createdPromotion(
            UUID promotionId,
            UUID productId,
            UUID buffetId
    ) {
        PromotionResponseDTO promotion = new PromotionResponseDTO();
        promotion.setId(promotionId);
        promotion.setName("Promo caliente");
        promotion.setDiscountPercentage(new BigDecimal("15.00"));
        promotion.setStartDate(LocalDateTime.of(2026, 7, 4, 8, 0));
        promotion.setEndDate(LocalDateTime.of(2026, 7, 5, 20, 0));
        promotion.setStatus("DRAFT");
        promotion.setProductIds(List.of(productId));
        promotion.setBuffetId(buffetId);
        promotion.setImageUrl("promo.png");
        return promotion;
    }

    private SeasonalRecommendationResponse execute(
            Double latitude,
            Double longitude,
            UUID buffetId
    ) {
        return useCase.execute(new GenerateSeasonalRecommendationRequest(
                latitude,
                longitude,
                buffetId
        ));
    }
}
