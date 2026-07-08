package com.recred.backend.unit.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.PriceAlertAIRepository;
import com.recred.backend.infrastructure.gemini.GeminiTextClient;
import com.recred.backend.infrastructure.gemini.GeminiTextResponse;
import com.recred.backend.infrastructure.persistence.GeminiPriceAlertAIAdapter;
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
    void analyzeImpact_SuccessfullyWhenGeminiRespondsWithValidJson() {
        givenGeminiResponse(validJsonResponse());

        Optional<PriceAlertAIRepository.PriceAlertResult> result =
                adapter.analyzeImpact(
                        createStudentDomain(),
                        createProductDomain(),
                        new BigDecimal("100.00"),
                        new BigDecimal("120.00")
                );

        assertTrue(result.isPresent());
        assertEquals("Price adjustment", result.get().titulo());
        assertEquals("20% change", result.get().razonIA());
    }

    @Test
    void returnFallback_WhenGeminiRespondsWithInvalidJson() {
        givenGeminiResponse("invalid");

        Optional<PriceAlertAIRepository.PriceAlertResult> result =
                adapter.analyzeImpact(
                        createStudentDomain(),
                        createProductDomain(),
                        new BigDecimal("100.00"),
                        new BigDecimal("120.00")
                );

        assertTrue(result.isPresent());
        assertEquals("Cambio de precio en Alfajor", result.get().titulo());
        assertEquals("Cambio automatico detectado de 20.00, equivalente a 20.00%.",
                result.get().razonIA());
    }

    @Test
    void returnFallback_WhenGeminiDoesNotGenerateResponse() {
        when(geminiTextClient.generate(anyString())).thenReturn(Optional.empty());

        Optional<PriceAlertAIRepository.PriceAlertResult> result =
                adapter.analyzeImpact(
                        createStudentDomain(),
                        createProductDomain(),
                        new BigDecimal("120.00"),
                        new BigDecimal("100.00")
                );

        assertTrue(result.isPresent());
        assertEquals("Cambio de precio en Alfajor", result.get().titulo());
        assertEquals("Cambio automatico detectado de -20.00, equivalente a -16.67%.",
                result.get().razonIA());
    }

    @Test
    void returnFallback_WhenGeminiResponseHasBlankFieldsAndOldPriceIsZero() {
        Alumno student = createStudentDomain();
        Grado grade = new Grado();
        grade.setAnio("1");
        grade.setDivision("A");
        student.setGrado(grade);
        givenGeminiResponse("""
                {
                  "mensaje": "Message",
                  "razonIA": "Reason"
                }
                """);

        Optional<PriceAlertAIRepository.PriceAlertResult> result =
                adapter.analyzeImpact(
                        student,
                        createProductDomain(),
                        BigDecimal.ZERO,
                        new BigDecimal("10.00")
                );

        assertTrue(result.isPresent());
        assertEquals("Cambio de precio en Alfajor", result.get().titulo());
        assertEquals("Cambio automatico detectado de 10.00, equivalente a 0%.",
                result.get().razonIA());
    }

    private void givenGeminiResponse(String text) {
        when(geminiTextClient.generate(anyString()))
                .thenReturn(Optional.of(new GeminiTextResponse(text, "model")));
    }

    private String validJsonResponse() {
        return """
                {
                  "titulo": "Price adjustment",
                  "mensaje": "We noticed the cookie price increased",
                  "razonIA": "20% change"
                }
                """;
    }

    private Alumno createStudentDomain() {
        Alumno student = new Alumno();
        student.setId(UUID.randomUUID());
        return student;
    }

    private Producto createProductDomain() {
        Producto product = new Producto();
        product.setNombre("Alfajor");
        return product;
    }
}
