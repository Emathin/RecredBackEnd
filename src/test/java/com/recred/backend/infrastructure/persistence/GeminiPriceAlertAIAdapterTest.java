package com.recred.backend.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.PriceAlertAIRepository;
import com.recred.backend.infrastructure.gemini.GeminiTextClient;
import com.recred.backend.infrastructure.gemini.GeminiTextResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiPriceAlertAIAdapterTest {

    @Mock
    private GeminiTextClient geminiTextClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private GeminiPriceAlertAIAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new GeminiPriceAlertAIAdapter(geminiTextClient, objectMapper);
    }

    @Test
    void analyzeImpactSuccessfullyWhenGeminiRespondsWithValidJson() {
        givenGeminiResponse(validJsonResponse());

        Optional<PriceAlertAIRepository.PriceAlertResult> result =
                adapter.analyzeImpact(
                        student(),
                        product(),
                        new BigDecimal("100.00"),
                        new BigDecimal("120.00")
                );

        assertTrue(result.isPresent());
        assertEquals("Ajuste de precio", result.get().titulo());
        assertEquals("Cambio del 20%", result.get().razonIA());
    }

    @Test
    void returnFallbackWhenGeminiRespondsWithInvalidJson() {
        givenGeminiResponse("invalid");

        Optional<PriceAlertAIRepository.PriceAlertResult> result =
                adapter.analyzeImpact(
                        student(),
                        product(),
                        new BigDecimal("100.00"),
                        new BigDecimal("120.00")
                );

        assertTrue(result.isPresent());
        assertEquals("Cambio de precio en Alfajor", result.get().titulo());
        assertEquals("Cambio automatico detectado de 20.00, equivalente a 20.00%.",
                result.get().razonIA());
    }

    @Test
    void returnFallbackWhenGeminiDoesNotGenerateResponse() {
        when(geminiTextClient.generate(anyString())).thenReturn(Optional.empty());

        Optional<PriceAlertAIRepository.PriceAlertResult> result =
                adapter.analyzeImpact(
                        student(),
                        product(),
                        new BigDecimal("120.00"),
                        new BigDecimal("100.00")
                );

        assertTrue(result.isPresent());
        assertEquals("Cambio de precio en Alfajor", result.get().titulo());
        assertEquals("Cambio automatico detectado de -20.00, equivalente a -16.67%.",
                result.get().razonIA());
    }

    private void givenGeminiResponse(String text) {
        when(geminiTextClient.generate(anyString()))
                .thenReturn(Optional.of(new GeminiTextResponse(text, "model")));
    }

    private String validJsonResponse() {
        return """
                {
                  "titulo": "Ajuste de precio",
                  "mensaje": "Notamos que el alfajor subio",
                  "razonIA": "Cambio del 20%"
                }
                """;
    }

    private Alumno student() {
        Alumno alumno = new Alumno();
        alumno.setId(UUID.randomUUID());
        return alumno;
    }

    private Producto product() {
        Producto producto = new Producto();
        producto.setNombre("Alfajor");
        return producto;
    }
}
