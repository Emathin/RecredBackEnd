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
import java.util.stream.Collectors;

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
        WeatherDTO currentWeather = climaService.getCurrentWeather(
                request.latitude(),
                request.longitude()
        );
        
        GeocodingResponseDTO geocoding = geocodingClient.getReverseGeocoding(
                request.latitude(),
                request.longitude()
        );
        if (geocoding != null && geocoding.getAddress() != null) {
            currentWeather.setCiudad(geocoding.getAddress().getCity());
            currentWeather.setProvincia(geocoding.getAddress().getState());
        }

        String monthName = LocalDate.now()
                .getMonth()
                .getDisplayName(TextStyle.FULL, new Locale("es", "AR"));
                
        String availableCategories = categoryRepository.getAllCategories().stream()
                .filter(CategoriaProducto::getActivo)
                .map(CategoriaProducto::getDescripcion)
                .collect(Collectors.joining(", "));

        RecommendationResponseDTO response = geminiSeasonalSuggesterService
                .getSeasonalRecommendations(
                        monthName,
                        currentWeather.getTemperature(),
                        currentWeather.getCondition(),
                        availableCategories);
                        
        SeasonInfoResponse seasonInfo = SeasonCalculator.calculateCurrentSeason(LocalDate.now());

        List<Producto> allActiveProducts = productRepository.findAllActiveWithStock(
                request.buffetId()
        );

        SuggestedPromotionDTO suggested = response.getSuggestedPromotion();
        if (suggested != null && suggested.getTargetCategories() != null 
                && !suggested.getTargetCategories().isEmpty()) {
            
            List<String> matchedProductIds = allActiveProducts.stream()
                    .filter(p -> p.getCategoria() != null)
                    .filter(p -> suggested.getTargetCategories().stream()
                            .anyMatch(cat -> p.getCategoria().getDescripcion()
                                    .toLowerCase().contains(cat.toLowerCase())))
                    .map(p -> p.getId().toString())
                    .limit(5)
                    .collect(Collectors.toList());
                    
            if (matchedProductIds.size() < 2) {
                matchedProductIds = allActiveProducts.stream()
                        .map(p -> p.getId().toString())
                        .limit(2)
                        .collect(Collectors.toList());
            }

            if (matchedProductIds.size() >= 2) {
                suggested.setProductIds(matchedProductIds);
            }
        }

        return toResponse(response, seasonInfo, currentWeather, allActiveProducts);
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
                .filter(product -> productMatchesCategory(product, suggestion.getCategoria()))
                .limit(3)
                .map(MenuProductResponse::permitido)
                .toList();
    }

    private boolean productMatchesCategory(Producto product, String category) {
        return product.getCategoria() != null
                && category != null
                && product.getCategoria().getDescripcion().toLowerCase()
                .contains(category.toLowerCase());
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
