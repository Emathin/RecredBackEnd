package com.recred.backend.infrastructure.gemini;

import com.recred.backend.infrastructure.web.RecomendacionResponseDTO;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class GeminiSeasonalSuggester {
        private final RestClient restClient;
        private final ObjectMapper objectMapper;

        @Value("${gemini.api.key}")
        private String apiKey;

        @Value("${gemini.model}")
        private String model;

        public GeminiSeasonalSuggester(ObjectMapper objectMapper) {
                this.restClient = RestClient.builder()
                                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                                .build();
                this.objectMapper = objectMapper;
        }

        public RecomendacionResponseDTO obtenerSugerenciasIas(
            String mes,
            String etapaEscolar,
            String nivelEscolar,
            Double temperatura,
            String condicionClimatica) {

                String prompt = armarPrompt(
                                mes,
                                etapaEscolar,
                                nivelEscolar,
                                temperatura,
                                condicionClimatica);

                Map<String, Object> requestBody = Map.of(
                                "contents", List.of(
                                                Map.of("parts", List.of(
                                                                Map.of("text", prompt)))),
                                "generationConfig", Map.of(
                                                "responseMimeType", "application/json"));

                try {
                        String rawResponse = restClient.post()
                                        .uri("/models/" + model + ":generateContent?key=" + apiKey)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(requestBody)
                                        .retrieve()
                                        .body(String.class);

                        JsonNode rootNode = objectMapper.readTree(rawResponse);
                        String jsonTextoIa = rootNode
                                        .path("candidates")
                                        .get(0)
                                        .path("content")
                                        .path("parts")
                                        .get(0)
                                        .path("text")
                                        .asText();

                        return objectMapper.readValue(jsonTextoIa, RecomendacionResponseDTO.class);

                } catch (Exception e) {
                        throw new RuntimeException(
                                        "Error al procesar la recomendación con Gemini: "
                                                        + e.getMessage(),
                                        e);
                }
        }

        private String armarPrompt(
                        String mes, String etapaEscolar, String nivelEscolar, Double temperatura,
                        String condicion) {
                return """
                        ROL:
                        Eres un sistema experto en retail y análisis comercial
                        para kioscos escolares.
                        Tu objetivo es procesar las condiciones climáticas y la
                        época del año para sugerir ajustes en las compras de
                        mercadería.

                        CONTEXTO ACTUAL:
                        - Fecha actual: %s (Etapa del ciclo lectivo: %s)
                        - Clima actual: %s°C, %s
                        - Tipo de establecimiento: Kiosco en escuela %s
                          (solo venta de alimentos y bebidas).

                        REGLAS DE NEGOCIO:
                        1. Evalúa el contexto y selecciona productos de las
                           siguientes categorías predefinidas:
                           - Alta Sensibilidad: Chocolates y Alfajores, Helados
                             y Congelados, Panificados y Horneados, Bebidas
                             Frías, Bebidas Calientes.
                           - Resistencia: Golosinas de Azúcar/Frutales,
                             Snacks Salados.
                           - Cuidado Especial: Lácteos y Frescos.
                        2. Identifica 2 categorías cuyo stock se deba AUMENTAR
                           por el clima/época.
                        3. Identifica 1 categoría cuyo stock se deba REDUCIR
                           por el clima/época.
                        4. Las justificaciones deben ser comerciales, breves
                           (máximo 15 palabras) y lógicas.

                        FORMATO DE SALIDA ESTRICTO:
                        No escribas saludos, introducciones ni texto adicional.
                        Tu única respuesta debe ser un objeto JSON válido con
                        la siguiente estructura exacta:

                        {
                          "sugerencias": [
                            {
                              "categoria": "Nombre de la categoría",
                              "accion": "AUMENTAR",
                              "motivo": "Justificación comercial breve."
                            }
                          ],
                          "tip_promocional": "Idea breve de promo cruzada."
                        }
                        """
                        .formatted(mes, etapaEscolar, temperatura, condicion,
                                nivelEscolar);
        }
}
