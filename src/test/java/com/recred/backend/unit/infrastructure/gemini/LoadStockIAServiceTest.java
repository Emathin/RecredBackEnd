package com.recred.backend.unit.infrastructure.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.LoadStockIARequest;
import com.recred.backend.application.LoadStockIAResponse;
import com.recred.backend.infrastructure.gemini.GeminiTextClient;
import com.recred.backend.infrastructure.gemini.GeminiTextResponse;
import com.recred.backend.infrastructure.gemini.LoadStockIAService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoadStockIAServiceTest {

    @Mock
    private GeminiTextClient geminiTextClient;

    private ObjectMapper objectMapper = new ObjectMapper();

    private LoadStockIAService loadStockIAService;

    @BeforeEach
    void setUp() {
        loadStockIAService = new LoadStockIAService(geminiTextClient, objectMapper);
    }

    @Test
    void analyze_DeberiaLanzarExcepcion_CuandoArchivoEstaVacio() {
        LoadStockIARequest request = new LoadStockIARequest(new byte[0], "test.jpg", "image/jpeg", 0);
        assertThrows(IllegalArgumentException.class, () -> loadStockIAService.analyze(request));
    }

    @Test
    void analyze_DeberiaLanzarExcepcion_CuandoFormatoNoPermitido() {
        LoadStockIARequest request = new LoadStockIARequest("content".getBytes(), "test.txt", "text/plain", "content".length());
        assertThrows(IllegalArgumentException.class, () -> loadStockIAService.analyze(request));
    }

    @Test
    void analyze_DeberiaLanzarExcepcion_CuandoContentTypeEsNulo() {
        LoadStockIARequest request = new LoadStockIARequest("content".getBytes(), "test", null, "content".length());
        assertThrows(IllegalArgumentException.class, () -> loadStockIAService.analyze(request));
    }

    @Test
    void analyze_DeberiaLanzarExcepcion_CuandoArchivoExcedeTamano() {
        byte[] largeContent = new byte[6 * 1024 * 1024];
        LoadStockIARequest request = new LoadStockIARequest(largeContent, "test.jpg", "image/jpeg", largeContent.length);
        assertThrows(IllegalArgumentException.class, () -> loadStockIAService.analyze(request));
    }

    @Test
    void analyze_DeberiaRetornarRespuestaVacia_CuandoIAnoGeneraRespuesta() {
        LoadStockIARequest request = new LoadStockIARequest("content".getBytes(), "test.jpg", "image/jpeg", "content".length());
        when(geminiTextClient.generateWithFile(anyString(), any(byte[].class), anyString()))
                .thenReturn(Optional.empty());

        LoadStockIAResponse result = loadStockIAService.analyze(request);

        assertEquals("-", result.name());
    }

    @Test
    void analyze_DeberiaLanzarExcepcion_CuandoIAFalla() {
        LoadStockIARequest request = new LoadStockIARequest("content".getBytes(), "test.jpg", "image/jpeg", "content".length());
        when(geminiTextClient.generateWithFile(anyString(), any(byte[].class), anyString()))
                .thenThrow(new RuntimeException("Error"));

        assertThrows(RuntimeException.class, () -> loadStockIAService.analyze(request));
    }

    @Test
    void analyze_DeberiaRetornarPrediccion_CuandoIAEsExitosa() {
        LoadStockIARequest request = new LoadStockIARequest("content".getBytes(), "test.jpg", "image/jpeg", "content".length());
        GeminiTextResponse geminiResponse = new GeminiTextResponse(
                "{\"nombre\":\"Galleta\",\"descripcion\":\"Pepitos\",\"peso\":\"100g\","
                        + "\"contiene_azucar\":\"si\",\"contiene_mani\":\"no\","
                        + "\"contiene_lactosa\":\"si\",\"contiene_tacc\":\"si\"}",
                "gemini-pro"
        );
        when(geminiTextClient.generateWithFile(anyString(), any(byte[].class), anyString()))
                .thenReturn(Optional.of(geminiResponse));

        LoadStockIAResponse result = loadStockIAService.analyze(request);

        assertNotNull(result);
        assertEquals("Galleta", result.name());
        assertEquals("Pepitos", result.description());
        assertEquals("100g", result.weight());
        assertEquals("si", result.containsSugar());
        assertEquals("no", result.containsPeanuts());
        assertEquals("si", result.containsLactose());
        assertEquals("si", result.containsGluten());
    }

    @Test
    void analyze_DeberiaRetornarRespuestaVacia_CuandoJsonEsInvalido() {
        LoadStockIARequest request = new LoadStockIARequest("content".getBytes(), "test.jpg", "image/jpeg", "content".length());
        GeminiTextResponse geminiResponse = new GeminiTextResponse("invalid json", "gemini-pro");
        when(geminiTextClient.generateWithFile(anyString(), any(byte[].class), anyString()))
                .thenReturn(Optional.of(geminiResponse));

        LoadStockIAResponse result = loadStockIAService.analyze(request);

        assertEquals("-", result.name());
    }
}

