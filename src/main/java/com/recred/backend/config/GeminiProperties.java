package com.recred.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        String apiKey,
        String model,
        boolean enabled
) {

    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
