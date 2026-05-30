package com.recred.backend.infrastructure.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.infrastructure.web.LoadStockPredictionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
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
        MockMultipartFile file = new MockMultipartFile("image", new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> loadStockIAService.analyze(file));
    }

    @Test
    void analyze_DeberiaLanzarExcepcion_CuandoFormatoNoPermitido() {
        MockMultipartFile file = new MockMultipartFile("image", "test.txt", "text/plain", "content".getBytes());
        assertThrows(IllegalArgumentException.class, () -> loadStockIAService.analyze(file));
    }

    @Test
    void analyze_DeberiaLanzarExcepcion_CuandoContentTypeEsNulo() {
        MockMultipartFile file = new MockMultipartFile("image", "test", null, "content".getBytes());
        assertThrows(IllegalArgumentException.class, () -> loadStockIAService.analyze(file));
    }

    @Test
    void analyze_DeberiaLanzarExcepcion_CuandoArchivoExcedeTamano() {
        byte[] largeContent = new byte[6 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", largeContent);
        assertThrows(IllegalArgumentException.class, () -> loadStockIAService.analyze(file));
    }

    @Test
    void analyze_DeberiaRetornarRespuestaVacia_CuandoIAnoGeneraRespuesta() {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "content".getBytes());
        when(geminiTextClient.generateWithImage(anyString(), any())).thenReturn(Optional.empty());

        LoadStockPredictionDTO result = loadStockIAService.analyze(file);

        assertEquals("-", result.getNombre());
    }

    @Test
    void analyze_DeberiaLanzarExcepcion_CuandoIAFalla() {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "content".getBytes());
        when(geminiTextClient.generateWithImage(anyString(), any())).thenThrow(new RuntimeException("Error"));

        assertThrows(RuntimeException.class, () -> loadStockIAService.analyze(file));
    }

    @Test
    void analyze_DeberiaRetornarPrediccion_CuandoIAEsExitosa() {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "content".getBytes());
        GeminiTextResponse geminiResponse = new GeminiTextResponse("{\"nombre\":\"Galleta\",\"descripcion\":\"Pepitos\",\"peso\":\"100g\",\"contiene_azucar\":\"si\",\"contiene_mani\":\"no\",\"contiene_lactosa\":\"si\",\"contiene_tacc\":\"si\"}", "gemini-pro");
        when(geminiTextClient.generateWithImage(anyString(), any())).thenReturn(Optional.of(geminiResponse));

        LoadStockPredictionDTO result = loadStockIAService.analyze(file);

        assertNotNull(result);
        assertEquals("Galleta", result.getNombre());
        assertEquals("Pepitos", result.getDescripcion());
        assertEquals("100g", result.getPeso());
        assertEquals("si", result.getContieneAzucar());
        assertEquals("no", result.getContieneMani());
        assertEquals("si", result.getContieneLactosa());
        assertEquals("si", result.getContieneTacc());
    }

    @Test
    void analyze_DeberiaRetornarRespuestaVacia_CuandoJsonEsInvalido() {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "content".getBytes());
        GeminiTextResponse geminiResponse = new GeminiTextResponse("invalid json", "gemini-pro");
        when(geminiTextClient.generateWithImage(anyString(), any())).thenReturn(Optional.of(geminiResponse));

        LoadStockPredictionDTO result = loadStockIAService.analyze(file);

        assertEquals("-", result.getNombre());
    }
}
