package com.recred.backend.infrastructure.gemini;

import com.google.genai.Client;
import com.google.genai.types.Blob;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.recred.backend.config.GeminiProperties;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class GoogleGeminiTextClient implements GeminiTextClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleGeminiTextClient.class);
    private static final String RESPONSE_MIME_TYPE = "application/json";
    private static final float TEMPERATURE = 0.2F;
    private static final int MAX_OUTPUT_TOKENS = 3000;
    private static final String VISION_MODEL = "gemini-3.1-flash-lite";


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
    public Optional<GeminiTextResponse> generateWithImage(String prompt, MultipartFile image) {
        if (!geminiProperties.isConfigured()) {
            return Optional.empty();
        }

        try {
            byte[] imageBytes = image.getBytes();
            String mimeType = Optional.ofNullable(image.getContentType())
                    .orElse("image/jpeg");

            try (Client client = Client.builder()
                    .apiKey(geminiProperties.apiKey())
                    .build()) {

                GenerateContentResponse response = generateVisionContent(
                        client, prompt, imageBytes, mimeType);

                return Optional.ofNullable(response.text())
                        .filter(text -> !text.isBlank())
                        .map(text -> new GeminiTextResponse(text, VISION_MODEL));
            }
        } catch (IOException ioException) {
            LOGGER.error("Error reading image file: {}", ioException.getMessage());
            return Optional.empty();
        } catch (RuntimeException exception) {
            LOGGER.warn("Gemini content generation with image failed: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    private GenerateContentResponse generateVisionContent(
            Client client, String prompt, byte[] imageBytes, String mimeType) {
        return client.models.generateContent(
                VISION_MODEL,
                buildMultimodalContent(prompt, imageBytes, mimeType),
                config()
        );
    }


    private Content buildMultimodalContent(String prompt, byte[] imageBytes, String mimeType) {
        Part imagePart = Part.builder()
                .inlineData(Blob.builder()
                        .data(imageBytes)
                        .mimeType(mimeType)
                        .build())
                .build();

        Part textPart = Part.builder()
                .text(prompt)
                .build();

        return Content.builder()
                .role("user")
                .parts(List.of(imagePart, textPart))
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
