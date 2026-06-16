package com.recred.backend.infrastructure.gemini;

import com.recred.backend.domain.services.ParentSummaryAnalyzer;
import org.springframework.stereotype.Component;

@Component
public class GeminiParentSummaryAnalyzer implements ParentSummaryAnalyzer {

    private final GeminiTextClient geminiTextClient;

    public GeminiParentSummaryAnalyzer(GeminiTextClient geminiTextClient) {
        this.geminiTextClient = geminiTextClient;
    }

    public String analyze(String resumenActual) {
        String prompt = "Analiza el siguiente resumen de gastos semanales " +
                "de los hijos de un padre, " +
                "considerando sus límites de presupuesto asignados: " +
                "Resumen actual: " + resumenActual +
                ". Límites de presupuesto por alumno: " +
                ". Genera un breve mensaje indicando indicando con porcentaje cuanto " +
                "de presupuesto ha ahorrado o no segun corresponda. " +
                "En el mensaje al utilizar cifras de gastos o limites anteponer el signo $" +
                "y eliminar decimales nulos. Introducir salto de linea al final de cada" +
                "oracion que introduscas en el mensaje" +
                "Responde solo JSON valido con esta forma:\n" +
                "mensaje\":\"[\\n  {\\n    " +
                "\\\"nombre\\\": \\\"NombreAlumno\\\",\\n    " +
                "\\\"mensaje\\\": \\\"" +
                "\\\"\\n  }}"

                ;

        return geminiTextClient.generate(prompt)
                .map(GeminiTextResponse::text)
                .orElse("No se pudo generar el análisis.");
    }
}
