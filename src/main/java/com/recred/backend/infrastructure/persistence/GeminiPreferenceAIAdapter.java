package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.PreferenceAIRepository;
import com.recred.backend.infrastructure.gemini.GeminiTextClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

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
    public Optional<PreferenciaDetectada> analizarPreferencias(
            Alumno alumno,
            List<CompraResumen> historial,
            List<Producto> stockDisponible) {

        String prompt = construirPrompt(alumno, historial, stockDisponible);

        return geminiTextClient.generate(prompt)
                .flatMap(response -> procesarRespuesta(response.text()));
    }

    private String construirPrompt(
            Alumno alumno,
            List<CompraResumen> historial,
            List<Producto> stockDisponible) {

        String historialStr = historial.stream()
                .map(c -> String.format("- %s (%s)", c.productoNombre(), c.categoria()))
                .collect(Collectors.joining("\n"));

        String stockStr = stockDisponible.stream()
                .map(p -> {
                    String catName = (p.getCategoria() != null)
                            ? p.getCategoria().getDescripcion()
                            : "Sin Categoria";
                    return String.format("- ID: %s, Nombre: %s, Categoria: %s",
                            p.getId(), p.getNombre(), catName);
                })
                .collect(Collectors.joining("\n"));

        return String.format("""
                Eres un experto en nutrición infantil y análisis de consumo para el proyecto Recred.
                Tu tarea es analizar el historial de compras de un alumno y detectar una preferencia
                que coincida con el stock actual del kiosco.
                
                HISTORIAL DE COMPRAS:
                %s
                
                STOCK DISPONIBLE HOY:
                %s
                
                REGLAS:
                1. Identifica un patrón claro (ej: prefiere snacks salados).
                2. Busca en el stock un producto que coincida con ese patrón.
                3. Si el producto es poco saludable, la razónIA debe mencionarlo.
                4. Responde ÚNICAMENTE en formato JSON con la siguiente estructura:
                   {
                     "titulo": "Título corto de la sugerencia",
                     "mensaje": "Mensaje para el tutor",
                     "productoId": "UUID del producto del stock",
                     "razonIA": "Explicación técnica"
                   }
                """, historialStr, stockStr);
    }

    private Optional<PreferenciaDetectada> procesarRespuesta(String text) {
        try {
            Map data = objectMapper.readValue(text, Map.class);

            return Optional.of(new PreferenciaDetectada(
                    (String) data.get("titulo"),
                    (String) data.get("mensaje"),
                    (String) data.get("productoId"),
                    (String) data.get("razonIA")
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
