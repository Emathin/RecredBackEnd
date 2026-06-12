package com.recred.backend.application;

import com.recred.backend.infrastructure.gemini.GeminiSeasonalSuggester;
import com.recred.backend.infrastructure.openMeteo.OpenMeteoWeatherClient;
import com.recred.backend.infrastructure.web.WeatherDTO;
import com.recred.backend.infrastructure.web.RecommendationResponseDTO;
import com.recred.backend.infrastructure.web.PromotionResponseDTO;
import com.recred.backend.infrastructure.web.SuggestedPromotionDTO;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.application.CreatePromotion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public class GenerateSeasonalRecommendation {
    private final OpenMeteoWeatherClient climaService;
    private final GeminiSeasonalSuggester geminiSeasonalSuggesterService;
    private final ProductRepository productRepository;
    private final CreatePromotion createPromotion;

    public GenerateSeasonalRecommendation(
            OpenMeteoWeatherClient climaService,
            GeminiSeasonalSuggester geminiSeasonalSuggesterService,
            ProductRepository productRepository,
            CreatePromotion createPromotion) {
        this.climaService = climaService;
        this.geminiSeasonalSuggesterService = geminiSeasonalSuggesterService;
        this.productRepository = productRepository;
        this.createPromotion = createPromotion;
    }

    public RecommendationResponseDTO execute(Double latitud, Double longitud) {
        WeatherDTO currentWeather = climaService.getCurrentWeather(latitud, longitud);
        String monthName = LocalDate.now()
                .getMonth()
                .getDisplayName(TextStyle.FULL, Locale.getDefault());

        RecommendationResponseDTO response = geminiSeasonalSuggesterService.getSeasonalRecommendations(
                monthName,
                currentWeather.getTemperature(),
                currentWeather.getCondition());

        SuggestedPromotionDTO suggested = response.getSuggestedPromotion();
        if (suggested != null && suggested.getTargetCategories() != null && !suggested.getTargetCategories().isEmpty()) {
            List<Producto> allActiveProducts = productRepository.findAllActive();
            
            List<UUID> matchedProductIds = allActiveProducts.stream()
                    .filter(p -> p.getCategoria() != null)
                    .filter(p -> suggested.getTargetCategories().stream()
                            .anyMatch(cat -> p.getCategoria().getDescripcion().toLowerCase().contains(cat.toLowerCase())))
                    .map(Producto::getId)
                    .limit(5)
                    .collect(Collectors.toList());
                    
            if (matchedProductIds.size() < 2) {
                matchedProductIds = allActiveProducts.stream()
                        .map(Producto::getId)
                        .limit(2)
                        .collect(Collectors.toList());
            }

            if (matchedProductIds.size() >= 2) {
                Promotion promotion = createPromotion.execute(
                        matchedProductIds,
                        suggested.getDiscountPercentage(),
                        suggested.getName(),
                        LocalDateTime.now(),
                        LocalDateTime.now().plusDays(7),
                        true
                );
                
                response.setCreatedPromotion(PromotionResponseDTO.fromDomain(promotion));
            }
        }

        return response;
    }
}
