package com.recred.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import com.recred.backend.infrastructure.gemini.GeminiSeasonalSuggester;
import com.recred.backend.infrastructure.gemini.GeminiTextClient;

@Configuration
public class GeminiSeasonalSuggesterConfig {

    @Bean
    public GeminiSeasonalSuggester geminiSeasonalSuggester(
            GeminiTextClient geminiTextClient,
            ObjectMapper objectMapper) {
        return new GeminiSeasonalSuggester(
                geminiTextClient,
                objectMapper);
    }
}
