package com.recred.backend.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.PriceAlertAIRepository;
import com.recred.backend.infrastructure.gemini.GeminiTextClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

@Component
public class GeminiPriceAlertAIAdapter implements PriceAlertAIRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeminiPriceAlertAIAdapter.class);
    private static final int PERCENTAGE_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");

    private final GeminiTextClient geminiTextClient;
    private final ObjectMapper objectMapper;

    public GeminiPriceAlertAIAdapter(GeminiTextClient geminiTextClient,
                                     ObjectMapper objectMapper) {
        this.geminiTextClient = geminiTextClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<PriceAlertResult> analyzeImpact(Alumno alumno,
                                                    Producto producto,
                                                    BigDecimal oldPrice,
                                                    BigDecimal newPrice) {

        String prompt = construirPrompt(alumno, producto, oldPrice, newPrice);

        return geminiTextClient.generate(prompt)
                .flatMap(response -> procesarRespuesta(response.text()))
                .or(() -> fallback(producto, oldPrice, newPrice));
    }

    private String construirPrompt(Alumno alumno, Producto producto,
                                   BigDecimal oldPrice, BigDecimal newPrice) {

        String gradoStr = (alumno.getGrado() != null)
                ? String.format("%s %s",
                alumno.getGrado().getAnio(), alumno.getGrado().getDivision())
                : "No especificado";

        String template = """
                Eres un asistente inteligente de finanzas familiares para el proyecto Recred.
                Un producto favorito de un alumno ha cambiado de precio y debes notificar al tutor.
                
                DATOS DEL ALUMNO:
                - Grado: %s
                
                PRODUCTO AFECTADO:
                - Nombre: %s
                - Precio Anterior: %s
                - Precio Nuevo: %s
                
                TAREA:
                Genera una alerta empática para el tutor. 
                Si el precio aumentó, indica el posible impacto en el presupuesto.
                Si bajó, dalo como una buena noticia.
                
                REGLAS:
                1. Responde ÚNICAMENTE en formato JSON.
                2. Estructura:
                   {
                     "titulo": "Título de la alerta",
                     "mensaje": "Mensaje para el tutor",
                     "razonIA": "Breve análisis técnico del impacto"
                   }
                """;

        return String.format(template, gradoStr, producto.getNombre(),
                oldPrice, newPrice);
    }

    private Optional<PriceAlertResult> procesarRespuesta(String text) {
        try {
            String cleanJson = text.replaceAll("```json|```", "").trim();
            Map data = objectMapper.readValue(cleanJson, Map.class);

            String titulo = (String) data.get("titulo");
            String mensaje = (String) data.get("mensaje");
            String razonIA = (String) data.get("razonIA");

            if (isBlank(titulo) || isBlank(mensaje) || isBlank(razonIA)) {
                return Optional.empty();
            }

            return Optional.of(new PriceAlertResult(titulo, mensaje, razonIA));
        } catch (Exception e) {
            LOGGER.warn("Gemini price alert response could not be parsed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<PriceAlertResult> fallback(
            Producto producto,
            BigDecimal oldPrice,
            BigDecimal newPrice
    ) {
        BigDecimal difference = newPrice.subtract(oldPrice);
        String direction = difference.signum() >= 0 ? "aumento" : "baja";
        String title = "Cambio de precio en " + producto.getNombre();
        String message = String.format(
                "El producto %s tuvo un %s de precio: paso de %s a %s.",
                producto.getNombre(),
                direction,
                oldPrice,
                newPrice
        );
        String reason = String.format(
                "Cambio automatico detectado de %s, equivalente a %s%%.",
                difference,
                percentageChange(oldPrice, difference)
        );
        return Optional.of(new PriceAlertResult(title, message, reason));
    }

    private BigDecimal percentageChange(BigDecimal oldPrice, BigDecimal difference) {
        if (oldPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return difference
                .multiply(ONE_HUNDRED)
                .divide(oldPrice, PERCENTAGE_SCALE, RoundingMode.HALF_UP);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
