package com.recred.backend.integration.config;

import com.recred.backend.config.GeminiProperties;
import com.recred.backend.config.SpendingPredictionConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpendingPredictionConfigTest {

    private final SpendingPredictionConfig config = new SpendingPredictionConfig();

    @Test
    void createPredictionBeans() {
        assertNotNull(config.applicationClock());
        assertNotNull(config.spendingPredictionRangeService());
        assertNotNull(config.spendingPredictionCalculator());
    }

    @Test
    void validateGeminiConfiguration() {
        assertTrue(new GeminiProperties("key", "gemini-test", true).isConfigured());
        assertFalse(new GeminiProperties("", "gemini-test", true).isConfigured());
        assertFalse(new GeminiProperties(null, "gemini-test", true).isConfigured());
        assertFalse(new GeminiProperties("key", "gemini-test", false).isConfigured());
    }
}
