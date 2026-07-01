package com.recred.backend.unit.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.PreferenceAIRepository;
import com.recred.backend.infrastructure.gemini.GeminiTextClient;
import com.recred.backend.infrastructure.gemini.GeminiTextResponse;
import com.recred.backend.infrastructure.persistence.GeminiPreferenceAIAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
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
    void analyzePreferences_SuccessfullyWhenGeminiRespondsWithValidJson() {
        Alumno student = new Alumno();
        student.setId(UUID.randomUUID());
        
        CategoriaProducto category = new CategoriaProducto();
        category.setDescripcion("Snacks");
        
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Chips");
        product.setCategoria(category);
        
        Producto productWithoutCategory = new Producto();
        productWithoutCategory.setId(UUID.randomUUID());
        productWithoutCategory.setNombre("Water");
        productWithoutCategory.setCategoria(null);

        PreferenceAIRepository.CompraResumen purchase = new PreferenceAIRepository.CompraResumen(
                "Cookies", "Sweets", LocalDateTime.now()
        );

        String jsonResponse = """
                {
                  "titulo": "Dairy Suggestion",
                  "mensaje": "Likes yogurts",
                  "productoId": "prod-123",
                  "razonIA": "Pattern detected"
                }
                """;
        
        when(geminiTextClient.generate(anyString()))
                .thenReturn(Optional.of(new GeminiTextResponse(jsonResponse, "model")));

        Optional<PreferenceAIRepository.PreferenciaDetectada> result = 
                adapter.analizarPreferencias(student, List.of(purchase), List.of(product, productWithoutCategory));

        assertTrue(result.isPresent());
        assertEquals("Dairy Suggestion", result.get().titulo());
        assertEquals("prod-123", result.get().productoId());
    }

    @Test
    void analyzePreferences_SuccessfullyEvenWhenGeminiWrapsJsonInMarkdownTags() {
        Alumno student = new Alumno();
        student.setId(UUID.randomUUID());
        
        String markdownResponse = """
                ```json
                {
                  "titulo": "Markdown Suggestion",
                  "mensaje": "Clean message",
                  "productoId": "prod-456",
                  "razonIA": "Cleanup logic"
                }
                ```
                """;
        
        when(geminiTextClient.generate(anyString()))
                .thenReturn(Optional.of(new GeminiTextResponse(markdownResponse, "model")));

        Optional<PreferenceAIRepository.PreferenciaDetectada> result = 
                adapter.analizarPreferencias(student, Collections.emptyList(), Collections.emptyList());

        assertTrue(result.isPresent());
        assertEquals("Markdown Suggestion", result.get().titulo());
        assertEquals("prod-456", result.get().productoId());
    }

    @Test
    void analyzePreferences_ShouldIncludeStudentGradeWhenPresent() {
        Alumno student = new Alumno();
        Grado grade = new Grado();
        grade.setAnio("3");
        grade.setDivision("B");
        student.setGrado(grade);
        when(geminiTextClient.generate(anyString())).thenReturn(Optional.empty());

        adapter.analizarPreferencias(student, Collections.emptyList(), Collections.emptyList());

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiTextClient).generate(promptCaptor.capture());
        assertTrue(promptCaptor.getValue().contains("- Grado: 3 B"));
    }

    @Test
    void returnEmptyOptional_WhenGeminiRespondsWithInvalidJson() {
        Alumno student = new Alumno();
        when(geminiTextClient.generate(anyString()))
                .thenReturn(Optional.of(new GeminiTextResponse("invalid json", "model")));

        Optional<PreferenceAIRepository.PreferenciaDetectada> result = 
                adapter.analizarPreferencias(student, Collections.emptyList(), Collections.emptyList());

        assertTrue(result.isEmpty());
    }

    @Test
    void returnEmptyOptional_WhenGeminiClientFailsToRespond() {
        Alumno student = new Alumno();
        when(geminiTextClient.generate(anyString())).thenReturn(Optional.empty());

        Optional<PreferenceAIRepository.PreferenciaDetectada> result = 
                adapter.analizarPreferencias(student, Collections.emptyList(), Collections.emptyList());

        assertTrue(result.isEmpty());
    }
}
