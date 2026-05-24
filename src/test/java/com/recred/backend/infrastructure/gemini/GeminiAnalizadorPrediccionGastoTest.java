package com.recred.backend.infrastructure.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.domain.models.AnalisisPrediccionGasto;
import com.recred.backend.domain.models.CategoriaConsumo;
import com.recred.backend.domain.models.PrediccionGasto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeminiAnalizadorPrediccionGastoTest {

    private final GeminiTextClient geminiTextClient = mock(GeminiTextClient.class);
    private final GeminiAnalizadorPrediccionGasto analizador =
            new GeminiAnalizadorPrediccionGasto(geminiTextClient, new ObjectMapper());

    @Test
    void parsearRespuestaJsonDeGemini() {
        when(geminiTextClient.generate(contains("gastoPredicho=120.00"))).thenReturn(
                Optional.of(new GeminiTextResponse("""
                        {
                          "resumen": "El gasto proyectado es alto.",
                          "alertas": ["Puede superar el presupuesto."],
                          "recomendaciones": ["Revisar compras de bebidas."]
                        }
                        """, "gemini-test"))
        );

        Optional<AnalisisPrediccionGasto> analisis = analizador.analizar(prediccion());

        assertTrue(analisis.isPresent());
        assertEquals("El gasto proyectado es alto.", analisis.orElseThrow().resumen());
        assertEquals("Puede superar el presupuesto.", analisis.orElseThrow().alertas().get(0));
        assertEquals("Revisar compras de bebidas.", analisis.orElseThrow().recomendaciones()
                .get(0));
        assertEquals("gemini-test", analisis.orElseThrow().modelo());
    }

    @Test
    void devolverVacioSiGeminiNoGeneraTexto() {
        when(geminiTextClient.generate(contains("gastoPredicho=120.00"))).thenReturn(
                Optional.empty()
        );

        Optional<AnalisisPrediccionGasto> analisis = analizador.analizar(prediccion());

        assertTrue(analisis.isEmpty());
    }

    @Test
    void devolverVacioSiGeminiDevuelveJsonInvalido() {
        when(geminiTextClient.generate(contains("gastoPredicho=120.00"))).thenReturn(
                Optional.of(new GeminiTextResponse("no es json", "gemini-test"))
        );

        Optional<AnalisisPrediccionGasto> analisis = analizador.analizar(prediccion());

        assertTrue(analisis.isEmpty());
    }

    @Test
    void normalizarListasNulasYElementosVacios() {
        when(geminiTextClient.generate(contains("gastoPredicho=120.00"))).thenReturn(
                Optional.of(new GeminiTextResponse("""
                        {
                          "resumen": "Resumen",
                          "alertas": null,
                          "recomendaciones": ["", "Comprar menos snacks.", null]
                        }
                        """, "gemini-test"))
        );

        Optional<AnalisisPrediccionGasto> analisis = analizador.analizar(prediccion());

        assertTrue(analisis.isPresent());
        assertTrue(analisis.orElseThrow().alertas().isEmpty());
        assertEquals(1, analisis.orElseThrow().recomendaciones().size());
        assertEquals("Comprar menos snacks.", analisis.orElseThrow().recomendaciones().get(0));
    }

    private PrediccionGasto prediccion() {
        return new PrediccionGasto(
                UUID.randomUUID(),
                PeriodoPresupuesto.MENSUAL,
                LocalDate.of(2026, 5, 21),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                new BigDecimal("50.00"),
                new BigDecimal("120.00"),
                new BigDecimal("10.00"),
                new BigDecimal("150.00"),
                new BigDecimal("80.00"),
                new BigDecimal("0.8000"),
                60,
                7,
                List.of(new CategoriaConsumo("Bebidas", new BigDecimal("40.00")))
        );
    }
}
