package com.recred.backend.infrastructure.ia.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrediccionGastoConfigTest {

    private final PrediccionGastoConfig config = new PrediccionGastoConfig();

    @Test
    void crearBeansDePrediccion() {
        assertNotNull(config.applicationClock());
        assertNotNull(config.rangoPrediccionGastoService());
        assertNotNull(config.prediccionGastoCalculator());
    }

    @Test
    void validarConfiguracionGemini() {
        assertTrue(new GeminiProperties("key", "gemini-test", true).isConfigured());
        assertFalse(new GeminiProperties("", "gemini-test", true).isConfigured());
        assertFalse(new GeminiProperties("key", "gemini-test", false).isConfigured());
    }
}
