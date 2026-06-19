package com.recred.backend.infrastructure.gemini;

import com.google.genai.Client;
import com.google.genai.types.Blob;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.recred.backend.config.GeminiProperties;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GoogleGeminiTextClient implements GeminiTextClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleGeminiTextClient.class);
    private static final String RESPONSE_MIME_TYPE = "application/json";
    private static final float TEMPERATURE = 0.2F;
    private static final int MAX_OUTPUT_TOKENS = 8192;


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
            LOGGER.warn("Gemini content generation failed: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<GeminiTextResponse> generateWithFile(String prompt, 
                                                         byte[] fileContent, 
                                                         String mimeType) {
        if (!geminiProperties.isConfigured()) {
            return Optional.empty();
        }

        try (Client client = Client.builder()
                .apiKey(geminiProperties.apiKey())
                .build()) {

            GenerateContentResponse response = generateVisionContent(
                    client, prompt, fileContent, mimeType);

            return Optional.ofNullable(response.text())
                    .filter(text -> !text.isBlank())
                    .map(text -> new GeminiTextResponse(text, geminiProperties.model()));
        } catch (RuntimeException exception) {
            LOGGER.warn("Gemini content generation with file failed: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    private GenerateContentResponse generateVisionContent(
            Client client, String prompt, byte[] fileBytes, String mimeType) {
        return client.models.generateContent(
                geminiProperties.model(),
                buildMultimodalContent(prompt, fileBytes, mimeType),
                config()
        );
    }


    private Content buildMultimodalContent(String prompt, byte[] fileBytes, String mimeType) {
        Part filePart = Part.builder()
                .inlineData(Blob.builder()
                        .data(fileBytes)
                        .mimeType(mimeType)
                        .build())
                .build();

        Part textPart = Part.builder()
                .text(prompt)
                .build();

        return Content.builder()
                .role("user")
                .parts(List.of(filePart, textPart))
                .build();
    }


    private GenerateContentConfig config() {
        return GenerateContentConfig.builder()
                .responseMimeType(RESPONSE_MIME_TYPE)
                .temperature(TEMPERATURE)
                .maxOutputTokens(MAX_OUTPUT_TOKENS)
                .build();
    }
}
