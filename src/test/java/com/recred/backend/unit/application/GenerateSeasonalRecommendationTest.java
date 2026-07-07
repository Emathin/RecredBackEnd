package com.recred.backend.unit.application;

import com.recred.backend.application.GenerateSeasonalRecommendation;
import com.recred.backend.application.GenerateSeasonalRecommendationRequest;
import com.recred.backend.application.SeasonalRecommendationResponse;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.CategoryRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.infrastructure.gemini.GeminiSeasonalSuggester;
import com.recred.backend.infrastructure.openMeteo.NominatimGeocodingClient;
import com.recred.backend.infrastructure.openMeteo.OpenMeteoWeatherClient;
import com.recred.backend.infrastructure.web.GeocodingResponseDTO;
import com.recred.backend.infrastructure.web.PromotionResponseDTO;
import com.recred.backend.infrastructure.web.RecommendationResponseDTO;
import com.recred.backend.infrastructure.web.SugerenciaDTO;
import com.recred.backend.infrastructure.web.SuggestedPromotionDTO;
import com.recred.backend.infrastructure.web.WeatherDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateSeasonalRecommendationTest {

    private static final double DEFAULT_LATITUDE = -34.7;
    private static final double DEFAULT_LONGITUDE = -58.47;
    private static final double TANDIL_LATITUDE = -37.32;
    private static final double TANDIL_LONGITUDE = -59.13;

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

    private GenerateSeasonalRecommendation useCase;
    private SeasonalRecommendationResponse response;
    private RuntimeException thrownException;
    private UUID buffetId;
    private UUID productId;
    private UUID secondProductId;
    private UUID promotionId;

    @BeforeEach
    void setUp() {
        useCase = new GenerateSeasonalRecommendation(
                climaServiceMock,
                geocodingClientMock,
                geminiSuggesterMock,
                productRepository,
                categoryRepository
        );
    }

    @Test
    void givenWeatherAndAiRespond_whenGeneratingRecommendation_thenReturnsSuggestions() {
        givenCurrentWeather(25.0, "Despejado");
        givenActiveCategories("Helados y Congelados");
        givenAiReturnsSuggestion("Helados y Congelados", "Promo 2x1 en helados.");
        givenNoActiveProducts();

        whenGenerateSeasonalRecommendation();

        thenPromotionalTipIs("Promo 2x1 en helados.");
        thenSuggestionCategoryIs("Helados y Congelados");
    }

    @Test
    void givenWeatherServiceFails_whenGeneratingRecommendation_thenThrowsException() {
        givenWeatherServiceFails("Weather service is currently unavailable");

        whenGenerateSeasonalRecommendationFails();

        thenErrorMessageIs("Weather service is currently unavailable");
        thenAiWasNotCalled();
    }

    @Test
    void givenAiServiceFails_whenGeneratingRecommendation_thenThrowsException() {
        givenCurrentWeather(5.0, "Frio Extremo");
        givenNoActiveCategories();
        givenAiServiceFails("Failed to generate seasonal recommendations from AI.");

        whenGenerateSeasonalRecommendationFails();

        thenErrorMessageIs("Failed to generate seasonal recommendations from AI.");
    }

    @Test
    void givenValidRequest_whenGeneratingRecommendation_thenCallsWeatherBeforeAi() {
        givenCurrentWeather(15.0, "Nublado");
        givenNoActiveCategories();
        givenAiReturnsEmptyRecommendation("Tip generico.");

        whenGenerateSeasonalRecommendation(10.0, 20.0, null);

        thenWeatherWasRequestedBeforeAi();
    }

    @Test
    void givenSuggestedPromotionWithMatchingProducts_whenGeneratingRecommendation_thenAddsProducts() {
        givenCurrentWeather(28.0, "Caluroso");
        givenNoActiveCategories();
        givenAiSuggestsPromotion("Promo fresca", "15.00", List.of("bebida"));
        givenActiveProductsWithStock("Bebidas frias", "Bebidas frias");

        whenGenerateSeasonalRecommendation();

        thenSuggestedPromotionHasTwoProducts();
        thenCreatedPromotionWasNotReturned();
    }

    @Test
    void givenSuggestedPromotionWithFewMatches_whenGeneratingRecommendation_thenUsesFallbackProducts() {
        givenCurrentWeather(10.0, "Frio");
        givenNoActiveCategories();
        givenAiSuggestsPromotion("Promo invierno", "20.00", List.of("caliente"));
        givenActiveProductsWithStock("Bebidas calientes", "Snacks");

        whenGenerateSeasonalRecommendation();

        thenSuggestedPromotionHasTwoProducts();
        thenCreatedPromotionWasNotReturned();
    }

    @Test
    void givenGeocodingAndCreatedPromotion_whenGeneratingRecommendation_thenMapsLocationAndPromotion() {
        givenBuffetForSeasonalRecommendation();
        givenCurrentWeather(12.5, "Frio");
        givenReverseGeocoding("Tandil", "Buenos Aires");
        givenActiveAndInactiveCategories();
        givenAiReturnsCreatedPromotion();
        givenActiveProductWithStock();

        whenGenerateSeasonalRecommendation(TANDIL_LATITUDE, TANDIL_LONGITUDE, buffetId);

        thenLocationWasMapped();
        thenSuggestedPromotionWasMappedWithoutProductIds();
        thenCreatedPromotionWasMapped();
    }

    private void givenCurrentWeather(double temperature, String condition) {
        when(climaServiceMock.getCurrentWeather(anyDouble(), anyDouble()))
                .thenReturn(new WeatherDTO(temperature, condition));
    }

    private void givenWeatherServiceFails(String message) {
        when(climaServiceMock.getCurrentWeather(anyDouble(), anyDouble()))
                .thenThrow(new RuntimeException(message));
    }

    private void givenActiveCategories(String categoryDescription) {
        when(categoryRepository.getAllCategories())
                .thenReturn(List.of(category(categoryDescription, true)));
    }

    private void givenNoActiveCategories() {
        when(categoryRepository.getAllCategories()).thenReturn(List.of());
    }

    private void givenActiveAndInactiveCategories() {
        when(categoryRepository.getAllCategories()).thenReturn(List.of(
                category("Infusiones", true),
                category("Oculta", false)
        ));
    }

    private void givenAiReturnsSuggestion(String category, String promotionalTip) {
        RecommendationResponseDTO recommendation = recommendationWithTip(promotionalTip);
        recommendation.setSugerences(List.of(
                new SugerenciaDTO(
                        category,
                        "AUMENTAR",
                        "Clima caluroso favorece ventas.",
                        "ice-cream"
                )
        ));

        when(geminiSuggesterMock.getSeasonalRecommendations(
                anyString(),
                anyDouble(),
                anyString(),
                anyString()))
                .thenReturn(recommendation);
    }

    private void givenAiReturnsEmptyRecommendation(String promotionalTip) {
        RecommendationResponseDTO recommendation = recommendationWithTip(promotionalTip);
        recommendation.setSugerences(List.of());

        when(geminiSuggesterMock.getSeasonalRecommendations(
                anyString(),
                anyDouble(),
                anyString(),
                anyString()))
                .thenReturn(recommendation);
    }

    private void givenAiSuggestsPromotion(
            String name,
            String discountPercentage,
            List<String> targetCategories
    ) {
        RecommendationResponseDTO recommendation = new RecommendationResponseDTO();
        recommendation.setSuggestedPromotion(suggestedPromotion(
                name,
                discountPercentage,
                targetCategories
        ));

        when(geminiSuggesterMock.getSeasonalRecommendations(
                anyString(),
                anyDouble(),
                anyString(),
                anyString()))
                .thenReturn(recommendation);
    }

    private void givenAiReturnsCreatedPromotion() {
        promotionId = randomUUID();
        productId = randomUUID();
        RecommendationResponseDTO recommendation = recommendationWithTip(
                "Sumar productos calientes.");
        recommendation.setSuggestedPromotion(suggestedPromotion(
                "Combo caliente",
                "10.00",
                List.of()
        ));
        recommendation.getSuggestedPromotion().setImagen("combo.png");
        recommendation.setCreatedPromotion(createdPromotion());

        when(geminiSuggesterMock.getSeasonalRecommendations(
                anyString(),
                eq(12.5),
                eq("Frio"),
                eq("Infusiones")))
                .thenReturn(recommendation);
    }

    private void givenAiServiceFails(String message) {
        when(geminiSuggesterMock.getSeasonalRecommendations(
                anyString(),
                anyDouble(),
                anyString(),
                anyString()))
                .thenThrow(new RuntimeException(message));
    }

    private void givenNoActiveProducts() {
        when(productRepository.findAllActiveWithStock(any())).thenReturn(List.of());
    }

    private void givenActiveProductsWithStock(
            String firstCategoryDescription,
            String secondCategoryDescription
    ) {
        productId = randomUUID();
        secondProductId = randomUUID();

        when(productRepository.findAllActiveWithStock(any())).thenReturn(List.of(
                product(productId, firstCategoryDescription),
                product(secondProductId, secondCategoryDescription)
        ));
    }

    private void givenActiveProductWithStock() {
        if (productId == null) {
            productId = randomUUID();
        }

        when(productRepository.findAllActiveWithStock(buffetId))
                .thenReturn(List.of(product(productId, "Infusiones")));
    }

    private void givenBuffetForSeasonalRecommendation() {
        buffetId = randomUUID();
    }

    private void givenReverseGeocoding(String town, String state) {
        when(geocodingClientMock.getReverseGeocoding(anyDouble(), anyDouble()))
                .thenReturn(geocoding(town, state));
    }

    private void whenGenerateSeasonalRecommendation() {
        response = execute(DEFAULT_LATITUDE, DEFAULT_LONGITUDE, null);
    }

    private void whenGenerateSeasonalRecommendation(
            double latitude,
            double longitude,
            UUID requestedBuffetId
    ) {
        response = execute(latitude, longitude, requestedBuffetId);
    }

    private void whenGenerateSeasonalRecommendationFails() {
        thrownException = assertThrows(
                RuntimeException.class,
                () -> execute(DEFAULT_LATITUDE, DEFAULT_LONGITUDE, null)
        );
    }

    private void thenPromotionalTipIs(String promotionalTip) {
        assertNotNull(response);
        assertEquals(promotionalTip, response.promotionalTip());
    }

    private void thenSuggestionCategoryIs(String category) {
        assertEquals(1, response.suggestions().size());
        assertEquals(category, response.suggestions().get(0).category());
    }

    private void thenErrorMessageIs(String message) {
        assertEquals(message, thrownException.getMessage());
    }

    private void thenAiWasNotCalled() {
        verify(geminiSuggesterMock, never()).getSeasonalRecommendations(
                anyString(),
                anyDouble(),
                anyString(),
                anyString()
        );
    }

    private void thenWeatherWasRequestedBeforeAi() {
        InOrder invocationOrder = inOrder(
                climaServiceMock,
                categoryRepository,
                geminiSuggesterMock
        );
        invocationOrder.verify(climaServiceMock).getCurrentWeather(10.0, 20.0);
        invocationOrder.verify(categoryRepository).getAllCategories();
        invocationOrder.verify(geminiSuggesterMock).getSeasonalRecommendations(
                anyString(),
                eq(15.0),
                eq("Nublado"),
                anyString()
        );
    }

    private void thenSuggestedPromotionHasTwoProducts() {
        assertNotNull(response.suggestedPromotion().productIds());
        assertEquals(2, response.suggestedPromotion().productIds().size());
    }

    private void thenCreatedPromotionWasNotReturned() {
        assertNull(response.createdPromotion());
    }

    private void thenLocationWasMapped() {
        assertNull(response.suggestions());
        assertEquals("Tandil", response.currentWeather().city());
        assertEquals("Buenos Aires", response.currentWeather().province());
    }

    private void thenSuggestedPromotionWasMappedWithoutProductIds() {
        assertEquals("Combo caliente", response.suggestedPromotion().name());
        assertEquals(List.of(), response.suggestedPromotion().targetCategories());
        assertNull(response.suggestedPromotion().productIds());
    }

    private void thenCreatedPromotionWasMapped() {
        assertEquals(promotionId, response.createdPromotion().id());
        assertEquals(productId, response.createdPromotion().productIds().get(0));
        assertEquals(buffetId, response.createdPromotion().buffetId());
    }

    private RecommendationResponseDTO recommendationWithTip(String promotionalTip) {
        RecommendationResponseDTO recommendation = new RecommendationResponseDTO();
        recommendation.setPromocionalTip(promotionalTip);
        return recommendation;
    }

    private SuggestedPromotionDTO suggestedPromotion(
            String name,
            String discountPercentage,
            List<String> targetCategories
    ) {
        SuggestedPromotionDTO suggestedPromotion = new SuggestedPromotionDTO();
        suggestedPromotion.setName(name);
        suggestedPromotion.setDiscountPercentage(new BigDecimal(discountPercentage));
        suggestedPromotion.setTargetCategories(targetCategories);
        return suggestedPromotion;
    }

    private Producto product(UUID requestedProductId, String categoryDescription) {
        Producto product = new Producto();
        product.setId(requestedProductId);
        product.setCategoria(category(categoryDescription, true));
        return product;
    }

    private CategoriaProducto category(String description, boolean active) {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(randomUUID());
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

    private PromotionResponseDTO createdPromotion() {
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
            double latitude,
            double longitude,
            UUID requestedBuffetId
    ) {
        return useCase.execute(new GenerateSeasonalRecommendationRequest(
                latitude,
                longitude,
                requestedBuffetId
        ));
    }
}
