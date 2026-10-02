package com.recred.backend.unit.infrastructure.gemini;

import com.recred.backend.infrastructure.gemini.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.PurchasePatternAnalysis;
import com.recred.backend.domain.models.PurchaseResume;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiPurchaseAnalyzerTest {

    @Mock
    private GeminiTextClient geminiTextClient;

    @Test
    void analyze_WhenGeminiReturnsValidJson_ShouldParseAnalysis() {
        UUID productId = UUID.randomUUID();
        GeminiPurchaseAnalyzer analyzer = analyzer();
        when(geminiTextClient.generate(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(new GeminiTextResponse("""
                        {
                          "resumen": "Compra yogur todos los lunes.",
                          "product_id": "%s",
                          "productName": "Yogur",
                          "patronDiasDetectado": "MONDAY"
                        }
                        """.formatted(productId), "gemini-test")));

        Optional<PurchasePatternAnalysis> result = analyzer.analyze(List.of(purchase()));

        assertTrue(result.isPresent());
        assertEquals("Compra yogur todos los lunes.", result.orElseThrow().summary());
        assertEquals(productId, result.orElseThrow().product_id());
        assertEquals("Yogur", result.orElseThrow().productName());
        assertEquals("MONDAY", result.orElseThrow().purchaseDays());
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiTextClient).generate(promptCaptor.capture());
        assertTrue(promptCaptor.getValue().contains("Yogur"));
    }

    @Test
    void analyze_WhenGeminiReturnsInvalidJson_ShouldReturnEmpty() {
        GeminiPurchaseAnalyzer analyzer = analyzer();
        when(geminiTextClient.generate(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(new GeminiTextResponse("no-json", "gemini-test")));

        Optional<PurchasePatternAnalysis> result = analyzer.analyze(List.of(purchase()));

        assertTrue(result.isEmpty());
    }

    @Test
    void analyze_WhenGeminiReturnsEmpty_ShouldThrowCurrentBehavior() {
        GeminiPurchaseAnalyzer analyzer = analyzer();
        when(geminiTextClient.generate(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());

        assertThrows(java.util.NoSuchElementException.class,
                () -> analyzer.analyze(List.of(purchase())));
    }

    @Test
    void normalizeList_ShouldRemoveNullAndBlankValues() {
        GeminiPurchaseAnalyzer analyzer = analyzer();

        List<String> result = ReflectionTestUtils.invokeMethod(
                analyzer,
                "normalizeList",
                Arrays.asList("uno", null, " ", "dos")
        );

        assertEquals(List.of("uno", "dos"), result);
        assertEquals(List.of(), ReflectionTestUtils.invokeMethod(
                analyzer,
                "normalizeList",
                (Object) null
        ));
    }

    private GeminiPurchaseAnalyzer analyzer() {
        return new GeminiPurchaseAnalyzer(geminiTextClient, new ObjectMapper());
    }

    private PurchaseResume purchase() {
        return new PurchaseResume(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Yogur",
                new BigDecimal("800.00"),
                LocalDateTime.of(2026, 6, 15, 10, 0)
        );
    }
}
