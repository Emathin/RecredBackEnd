package com.recred.backend.infrastructure.gemini;

import org.springframework.stereotype.Component;

@Component
public class GeminiParentSummaryAnalyzer {

    private final GeminiTextClient geminiTextClient;

    public GeminiParentSummaryAnalyzer(GeminiTextClient geminiTextClient) {
        this.geminiTextClient = geminiTextClient;
    }

    public String analyze(String resumenAnterior, String resumenActual) {
        String prompt = "Analiza los siguientes dos resúmenes de gastos semanales " +
                "de hijos de un padre. El primero es de la semana pasada, " +
                "el segundo de esta semana. Genera un breve mensaje comparativo " +
                "explicando si hubo ahorro o incremento de gastos, enfocándote en " +
                "cambios notables. Siempre que haya ahorro o incremento de " +
                "gastos mencionar el porcentaje correspondiente" +
                " Resumen anterior: " + resumenAnterior +
                ". Resumen actual: " + resumenActual;

        return geminiTextClient.generate(prompt)
                .map(GeminiTextResponse::text)
                .orElse("No se pudo generar el análisis comparativo.");
    }
}
