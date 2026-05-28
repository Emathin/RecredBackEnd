package com.recred.backend.application;

import com.recred.backend.infrastructure.gemini.GeminiSeasonalSuggester;
import com.recred.backend.infrastructure.openMeteo.OpenMeteoWeatherClient;
import com.recred.backend.infrastructure.web.WeatherDTO;
import com.recred.backend.infrastructure.web.RecommendationResponseDTO;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

import org.springframework.stereotype.Service;

@Service
public class GenerateSeasonalRecommendation {
    private final OpenMeteoWeatherClient climaService;
    private final GeminiSeasonalSuggester geminiSeasonalSuggesterService;

    public GenerateSeasonalRecommendation(
            OpenMeteoWeatherClient climaService,
            GeminiSeasonalSuggester geminiSeasonalSuggesterService) {
        this.climaService = climaService;
        this.geminiSeasonalSuggesterService = geminiSeasonalSuggesterService;
    }

    public RecommendationResponseDTO execute(Double latitud, Double longitud) {
        WeatherDTO currentWeather = climaService.getCurrentWeather(latitud, longitud);
        String monthName = LocalDate.now()
                .getMonth()
                .getDisplayName(TextStyle.FULL, Locale.getDefault());

        return geminiSeasonalSuggesterService.getSeasonalRecommendations(
                monthName,
                currentWeather.getTemperature(),
                currentWeather.getCondition());
    }
}
