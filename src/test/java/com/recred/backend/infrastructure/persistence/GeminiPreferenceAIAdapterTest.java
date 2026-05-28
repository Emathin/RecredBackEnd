package com.recred.backend.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.PreferenceAIRepository;
import com.recred.backend.infrastructure.gemini.GeminiTextClient;
import com.recred.backend.infrastructure.gemini.GeminiTextResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
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
    void analyzePreferencesSuccessfullyWhenGeminiRespondsWithValidJson() {
        Alumno alumno = new Alumno();
        alumno.setId(UUID.randomUUID());
        
        com.recred.backend.domain.models.CategoriaProducto categoria = new com.recred.backend.domain.models.CategoriaProducto();
        categoria.setDescripcion("Snacks");
        
        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());
        producto.setNombre("Papas");
        producto.setCategoria(categoria);
        
        Producto productoSinCat = new Producto();
        productoSinCat.setId(UUID.randomUUID());
        productoSinCat.setNombre("Agua");
        productoSinCat.setCategoria(null);

        PreferenceAIRepository.CompraResumen compra = new PreferenceAIRepository.CompraResumen(
                "Galletitas", "Dulces", java.time.LocalDateTime.now()
        );

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
                adapter.analizarPreferencias(alumno, List.of(compra), List.of(producto, productoSinCat));

        assertTrue(result.isPresent());
        assertEquals("Sugerencia Lácteos", result.get().titulo());
        assertEquals("prod-123", result.get().productoId());
    }

    @Test
    void returnEmptyOptionalWhenGeminiRespondsWithInvalidJson() {
        Alumno alumno = new Alumno();
        when(geminiTextClient.generate(anyString()))
                .thenReturn(Optional.of(new GeminiTextResponse("invalid json", "model")));

        Optional<PreferenceAIRepository.PreferenciaDetectada> result = 
                adapter.analizarPreferencias(alumno, Collections.emptyList(), Collections.emptyList());

        assertTrue(result.isEmpty());
    }

    @Test
    void returnEmptyOptionalWhenGeminiClientFailsToRespond() {
        Alumno alumno = new Alumno();
        when(geminiTextClient.generate(anyString())).thenReturn(Optional.empty());

        Optional<PreferenceAIRepository.PreferenciaDetectada> result = 
                adapter.analizarPreferencias(alumno, Collections.emptyList(), Collections.emptyList());

        assertTrue(result.isEmpty());
    }
}
