package com.recred.backend.application;

import com.recred.backend.infrastructure.gemini.GeminiSeasonalSuggester;
import com.recred.backend.infrastructure.openMeteo.NominatimGeocodingClient;
import com.recred.backend.infrastructure.openMeteo.OpenMeteoWeatherClient;
import com.recred.backend.infrastructure.web.GeocodingResponseDTO;
import com.recred.backend.infrastructure.web.RecommendationResponseDTO;
import com.recred.backend.infrastructure.web.SuggestedPromotionDTO;
import com.recred.backend.infrastructure.web.WeatherDTO;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.repositories.CategoryRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.infrastructure.web.PromotionResponseDTO;
import com.recred.backend.infrastructure.web.SugerenciaDTO;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class GenerateSeasonalRecommendation {
    private final OpenMeteoWeatherClient climaService;
    private final NominatimGeocodingClient geocodingClient;
    private final GeminiSeasonalSuggester geminiSeasonalSuggesterService;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public GenerateSeasonalRecommendation(
            OpenMeteoWeatherClient climaService,
            NominatimGeocodingClient geocodingClient,
            GeminiSeasonalSuggester geminiSeasonalSuggesterService,
            ProductRepository productRepository,
            CategoryRepository categoryRepository) {
        this.climaService = climaService;
        this.geocodingClient = geocodingClient;
        this.geminiSeasonalSuggesterService = geminiSeasonalSuggesterService;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public SeasonalRecommendationResponse execute(GenerateSeasonalRecommendationRequest request) {
        WeatherDTO currentWeather = currentWeather(request);
        RecommendationResponseDTO response = recommendationFor(currentWeather);
        List<Producto> products = activeProducts(request);
        applySuggestedPromotionProducts(response, products);
        SeasonInfoResponse seasonInfo = SeasonCalculator.calculateCurrentSeason(LocalDate.now());
        return toResponse(response, seasonInfo, currentWeather, products);
    }

    private WeatherDTO currentWeather(GenerateSeasonalRecommendationRequest request) {
        WeatherDTO currentWeather = climaService.getCurrentWeather(
                request.latitude(), request.longitude());
        GeocodingResponseDTO geocoding = geocodingClient.getReverseGeocoding(
                request.latitude(), request.longitude());
        if (geocoding != null && geocoding.getAddress() != null) {
            currentWeather.setCiudad(geocoding.getAddress().getCity());
            currentWeather.setProvincia(geocoding.getAddress().getState());
        }
        return currentWeather;
    }

    private RecommendationResponseDTO recommendationFor(WeatherDTO currentWeather) {
        return geminiSeasonalSuggesterService.getSeasonalRecommendations(
                currentMonthName(),
                currentWeather.getTemperature(),
                currentWeather.getCondition(),
                availableCategories());
    }

    private String currentMonthName() {
        return LocalDate.now()
                .getMonth()
                .getDisplayName(TextStyle.FULL, new Locale("es", "AR"));
    }

    private String availableCategories() {
        return categoryRepository.getAllCategories().stream()
                .filter(CategoriaProducto::getActivo)
                .map(CategoriaProducto::getDescripcion)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private List<Producto> activeProducts(GenerateSeasonalRecommendationRequest request) {
        return productRepository.findAllActiveWithStock(request.buffetId());
    }

    private void applySuggestedPromotionProducts(
            RecommendationResponseDTO response,
            List<Producto> products
    ) {
        SuggestedPromotionDTO suggested = response.getSuggestedPromotion();
        if (!hasTargetCategories(suggested)) {
            return;
        }

        List<String> matchedProductIds = matchedProductIds(products, suggested);
        if (matchedProductIds.size() < 2) {
            matchedProductIds = fallbackProductIds(products);
        }

        if (matchedProductIds.size() >= 2) {
            suggested.setProductIds(matchedProductIds);
        }
    }

    private boolean hasTargetCategories(SuggestedPromotionDTO suggested) {
        return suggested != null
                && suggested.getTargetCategories() != null
                && !suggested.getTargetCategories().isEmpty();
    }

    private List<String> matchedProductIds(
            List<Producto> products,
            SuggestedPromotionDTO suggested
    ) {
        return products.stream()
                .filter(product -> product.matchesAnyCategoryDescription(
                        suggested.getTargetCategories()))
                .map(Producto::idAsString)
                .limit(5)
                .toList();
    }

    private List<String> fallbackProductIds(List<Producto> products) {
        return products.stream()
                .map(Producto::idAsString)
                .limit(2)
                .toList();
    }

    private SeasonalRecommendationResponse toResponse(
            RecommendationResponseDTO response,
            SeasonInfoResponse seasonInfo,
            WeatherDTO currentWeather,
            List<Producto> products
    ) {
        return new SeasonalRecommendationResponse(
                suggestions(response, products),
                response.getPromocionalTip(),
                suggestedPromotion(response.getSuggestedPromotion()),
                createdPromotion(response.getCreatedPromotion()),
                seasonInfo,
                currentWeather(currentWeather)
        );
    }

    private List<SeasonalSuggestionResponse> suggestions(
            RecommendationResponseDTO response,
            List<Producto> products
    ) {
        if (response.getSugerences() == null) {
            return null;
        }

        return response.getSugerences().stream()
                .map(suggestion -> suggestion(suggestion, products))
                .toList();
    }

    private SeasonalSuggestionResponse suggestion(
            SugerenciaDTO suggestion,
            List<Producto> products
    ) {
        return new SeasonalSuggestionResponse(
                suggestion.getCategoria(),
                suggestion.getAccion(),
                suggestion.getMotivo(),
                suggestion.getIcono(),
                topProducts(suggestion, products)
        );
    }

    private List<MenuProductResponse> topProducts(
            SugerenciaDTO suggestion,
            List<Producto> products
    ) {
        return products.stream()
                .filter(product -> product.matchesCategoryDescription(suggestion.getCategoria()))
                .limit(3)
                .map(MenuProductResponse::permitido)
                .toList();
    }

    private SuggestedPromotionResponse suggestedPromotion(SuggestedPromotionDTO suggested) {
        if (suggested == null) {
            return null;
        }

        return new SuggestedPromotionResponse(
                suggested.getName(),
                suggested.getDiscountPercentage(),
                suggested.getTargetCategories(),
                suggested.getImagen(),
                suggested.getProductIds()
        );
    }

    private SeasonalCreatedPromotionResponse createdPromotion(PromotionResponseDTO created) {
        if (created == null) {
            return null;
        }

        return new SeasonalCreatedPromotionResponse(
                created.getId(),
                created.getName(),
                created.getDiscountPercentage(),
                created.getStartDate(),
                created.getEndDate(),
                created.getStatus(),
                created.getProductIds(),
                created.getBuffetId(),
                created.getImageUrl()
        );
    }

    private WeatherResponse currentWeather(WeatherDTO currentWeather) {
        return new WeatherResponse(
                currentWeather.getTemperature(),
                currentWeather.getCondition(),
                currentWeather.getCiudad(),
                currentWeather.getProvincia()
        );
    }
}
