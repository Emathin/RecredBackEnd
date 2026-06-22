package com.recred.backend.application;

import com.recred.backend.infrastructure.gemini.GeminiSeasonalSuggester;
import com.recred.backend.infrastructure.openMeteo.NominatimGeocodingClient;
import com.recred.backend.infrastructure.openMeteo.OpenMeteoWeatherClient;
import com.recred.backend.infrastructure.web.GeocodingResponseDTO;
import com.recred.backend.infrastructure.web.WeatherDTO;
import com.recred.backend.infrastructure.web.RecommendationResponseDTO;
import com.recred.backend.infrastructure.web.SuggestedPromotionDTO;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.repositories.CategoryRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.infrastructure.web.SeasonInfoDTO;
import com.recred.backend.infrastructure.web.MenuProductoDTO;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
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

    public RecommendationResponseDTO execute(Double latitud, Double longitud, UUID buffetId) {
        WeatherDTO currentWeather = climaService.getCurrentWeather(latitud, longitud);
        
        GeocodingResponseDTO geocoding = geocodingClient.getReverseGeocoding(latitud, longitud);
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
                        
        SeasonInfoDTO seasonInfo = SeasonCalculator.calculateCurrentSeason(LocalDate.now());
        response.setSeasonInfo(seasonInfo);
        response.setCurrentWeather(currentWeather);

        List<Producto> allActiveProducts = productRepository.findAllActive();

        if (response.getSugerences() != null) {
            for (var sugerencia : response.getSugerences()) {
                List<MenuProductoDTO> topProductos = allActiveProducts.stream()
                        .filter(p -> p.getCategoria() != null)
                        .filter(p -> p.getCategoria().getDescripcion().toLowerCase()
                                .contains(sugerencia.getCategoria().toLowerCase()))
                        .limit(3)
                        .map(MenuProductoDTO::permitido)
                        .collect(Collectors.toList());
                sugerencia.setTopProductos(topProductos);
            }
        }

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

        return response;
    }
}
