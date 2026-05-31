package com.recred.backend.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.models.Product;
import com.recred.backend.domain.repositories.PreferenceAIRepository;
import com.recred.backend.infrastructure.gemini.GeminiTextClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class GeminiPreferenceAIAdapter implements PreferenceAIRepository {

    private final GeminiTextClient geminiTextClient;
    private final ObjectMapper objectMapper;

    public GeminiPreferenceAIAdapter(GeminiTextClient geminiTextClient,
                                     ObjectMapper objectMapper) {
        this.geminiTextClient = geminiTextClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<DetectedPreference> analizarPreferencias(
            Student student,
            List<PurchaseSummary> history,
            List<Product> availableStock) {

        String prompt = construirPrompt(student, history, availableStock);

        return geminiTextClient.generate(prompt)
                .flatMap(response -> procesarRespuesta(response.text()));
    }

    private String construirPrompt(
            Student student,
            List<PurchaseSummary> historial,
            List<Product> stockDisponible) {

        String gradoStr = (student.getGrade() != null)
                ? String.format("%s %s",
                student.getGrade().getAnio(), student.getGrade().getDivision())
                : "No especificado";

        String historialStr = historial.stream()
                .map(c -> String.format("- %s (%s)", c.productName(), c.category()))
                .collect(Collectors.joining("\n"));

        String stockStr = stockDisponible.stream()
                .map(p -> {
                    String catName = (p.getCategory() != null)
                            ? p.getCategory().getDescription()
                            : "Sin Categoria";
                    return String.format("- ID: %s, Nombre: %s, Categoria: %s",
                            p.getId(), p.getName(), catName);
                })
                .collect(Collectors.joining("\n"));

        return String.format("""
                Eres un experto en nutrición infantil y análisis de consumo para el proyecto Recred.
                Tu tarea es analizar el historial de compras de un student y detectar una
                preferencia que coincida con el stock actual del kiosco.
                
                DATOS DEL ALUMNO:
                - Grado: %s
                
                HISTORIAL DE COMPRAS:
                %s
                
                STOCK DISPONIBLE HOY:
                %s
                
                REGLAS:
                1. Identifica un patrón claro (ej: prefiere snacks salados).
                2. Busca en el stock un product que coincida con ese patrón.
                3. Si el product es poco saludable, la razónIA debe mencionarlo.
                4. Responde ÚNICAMENTE en formato JSON con la siguiente estructura:
                   {
                     "tittle": "Título corto de la sugerencia",
                     "message": "Mensaje para el tutor",
                     "productId": "UUID del product del stock",
                     "razonIA": "Explicación técnica"
                   }
                """, gradoStr, historialStr, stockStr);
    }

    private Optional<DetectedPreference> procesarRespuesta(String text) {
        try {
            String cleanJson = text.replaceAll("```json|```", "").trim();
            Map data = objectMapper.readValue(cleanJson, Map.class);

            return Optional.of(new DetectedPreference(
                    (String) data.get("tittle"),
                    (String) data.get("message"),
                    (String) data.get("productId"),
                    (String) data.get("razonIA")
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
