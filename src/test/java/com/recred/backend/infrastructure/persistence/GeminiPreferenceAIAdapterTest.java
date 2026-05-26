package com.recred.backend.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.PreferenceAIRepository;
import com.recred.backend.infrastructure.gemini.GeminiTextClient;
import com.recred.backend.infrastructure.gemini.GeminiTextResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiPreferenceAIAdapterTest {

    @Mock
    private GeminiTextClient geminiTextClient;
    
    private ObjectMapper objectMapper = new ObjectMapper();
    private GeminiPreferenceAIAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new GeminiPreferenceAIAdapter(geminiTextClient, objectMapper);
    }

    @Test
    void analizaPreferencias_Correctamente_CuandoGeminiRespondeJSONValido() {
        Alumno alumno = new Alumno();
        alumno.setId(UUID.randomUUID());
        
        String jsonResponse = """
                {
                  "titulo": "Sugerencia Lácteos",
                  "mensaje": "Le gustan los yogures",
                  "productoId": "prod-123",
                  "razonIA": "Patrón detectado"
                }
                """;
        
        when(geminiTextClient.generate(anyString()))
                .thenReturn(Optional.of(new GeminiTextResponse(jsonResponse, "model")));

        Optional<PreferenceAIRepository.PreferenciaDetectada> result = 
                adapter.analizarPreferencias(alumno, Collections.emptyList(), Collections.emptyList());

        assertTrue(result.isPresent());
        assertEquals("Sugerencia Lácteos", result.get().titulo());
        assertEquals("prod-123", result.get().productoId());
    }

    @Test
    void retornaVacio_CuandoGeminiRespondeJSONInvalido() {
        Alumno alumno = new Alumno();
        when(geminiTextClient.generate(anyString()))
                .thenReturn(Optional.of(new GeminiTextResponse("invalid json", "model")));

        Optional<PreferenceAIRepository.PreferenciaDetectada> result = 
                adapter.analizarPreferencias(alumno, Collections.emptyList(), Collections.emptyList());

        assertTrue(result.isEmpty());
    }

    @Test
    void retornaVacio_CuandoGeminiClient_Falla() {
        Alumno alumno = new Alumno();
        when(geminiTextClient.generate(anyString())).thenReturn(Optional.empty());

        Optional<PreferenceAIRepository.PreferenciaDetectada> result = 
                adapter.analizarPreferencias(alumno, Collections.emptyList(), Collections.emptyList());

        assertTrue(result.isEmpty());
    }
}
