package com.recred.backend.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.Product;
import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.models.ProductCategory;
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
        Student student = new Student();
        student.setId(UUID.randomUUID());
        
        ProductCategory categoria = new ProductCategory();
        categoria.setDescription("Snacks");
        
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Papas");
        product.setCategory(categoria);
        
        Product productSinCat = new Product();
        productSinCat.setId(UUID.randomUUID());
        productSinCat.setName("Agua");
        productSinCat.setCategory(null);

        PreferenceAIRepository.PurchaseSummary compra = new PreferenceAIRepository.PurchaseSummary(
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

        Optional<PreferenceAIRepository.DetectedPreference> result =
                adapter.analizarPreferencias(student, List.of(compra), List.of(product, productSinCat));

        assertTrue(result.isPresent());
        assertEquals("Sugerencia Lácteos", result.get().tittle());
        assertEquals("prod-123", result.get().productoId());
    }

    @Test
    void analyzePreferencesSuccessfullyEvenWhenGeminiWrapsJsonInMarkdownTags() {
        Student student = new Student();
        student.setId(UUID.randomUUID());
        
        String markdownResponse = """
                ```json
                {
                  "titulo": "Sugerencia Markdown",
                  "mensaje": "Mensaje limpio",
                  "productoId": "prod-456",
                  "razonIA": "Lógica de limpieza"
                }
                ```
                """;
        
        when(geminiTextClient.generate(anyString()))
                .thenReturn(Optional.of(new GeminiTextResponse(markdownResponse, "model")));

        Optional<PreferenceAIRepository.DetectedPreference> result =
                adapter.analizarPreferencias(student, Collections.emptyList(), Collections.emptyList());

        assertTrue(result.isPresent());
        assertEquals("Sugerencia Markdown", result.get().tittle());
        assertEquals("prod-456", result.get().productoId());
    }

    @Test
    void returnEmptyOptionalWhenGeminiRespondsWithInvalidJson() {
        Student student = new Student();
        when(geminiTextClient.generate(anyString()))
                .thenReturn(Optional.of(new GeminiTextResponse("invalid json", "model")));

        Optional<PreferenceAIRepository.DetectedPreference> result =
                adapter.analizarPreferencias(student, Collections.emptyList(), Collections.emptyList());

        assertTrue(result.isEmpty());
    }

    @Test
    void returnEmptyOptionalWhenGeminiClientFailsToRespond() {
        Student student = new Student();
        when(geminiTextClient.generate(anyString())).thenReturn(Optional.empty());

        Optional<PreferenceAIRepository.DetectedPreference> result =
                adapter.analizarPreferencias(student, Collections.emptyList(), Collections.emptyList());

        assertTrue(result.isEmpty());
    }
}
