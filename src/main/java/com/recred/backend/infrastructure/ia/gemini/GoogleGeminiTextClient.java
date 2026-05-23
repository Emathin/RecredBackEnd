package com.recred.backend.infrastructure.ia.gemini;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.recred.backend.infrastructure.ia.config.GeminiProperties;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GoogleGeminiTextClient implements GeminiTextClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleGeminiTextClient.class);

    private final GeminiProperties geminiProperties;

    public GoogleGeminiTextClient(GeminiProperties geminiProperties) {
        this.geminiProperties = geminiProperties;
    }

    @Override
    public Optional<GeminiTextResponse> generate(String prompt) {
        if (!geminiProperties.isConfigured()) {
            return Optional.empty();
        }

        try (Client client = Client.builder()
                .apiKey(geminiProperties.apiKey())
                .build()) {
            GenerateContentResponse response = client.models.generateContent(
                    geminiProperties.model(),
                    prompt,
                    config()
            );
            return Optional.ofNullable(response.text())
                    .filter(text -> !text.isBlank())
                    .map(text -> new GeminiTextResponse(text, geminiProperties.model()));
        } catch (RuntimeException exception) {
            LOGGER.warn("No se pudo generar el analisis IA de prediccion de gasto", exception);
            return Optional.empty();
        }
    }

    private GenerateContentConfig config() {
        return GenerateContentConfig.builder()
                .responseMimeType("application/json")
                .temperature(0.2F)
                .maxOutputTokens(500)
                .build();
    }
}
