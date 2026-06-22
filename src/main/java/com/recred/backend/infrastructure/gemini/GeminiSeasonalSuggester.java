package com.recred.backend.infrastructure.gemini;

import com.recred.backend.infrastructure.web.RecommendationResponseDTO;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeminiSeasonalSuggester {

  private static final Logger LOGGER = LoggerFactory.getLogger(GeminiSeasonalSuggester.class);
  private final GeminiTextClient geminiTextClient;
  private final ObjectMapper objectMapper;

  public GeminiSeasonalSuggester(GeminiTextClient geminiTextClient, ObjectMapper objectMapper) {
    this.geminiTextClient = geminiTextClient;
    this.objectMapper = objectMapper;
  }

  public RecommendationResponseDTO getSeasonalRecommendations(
      String month,
      Double temperature,
      String weatherCondition,
      String availableCategories) {

    String prompt = buildPrompt(month, temperature, weatherCondition, availableCategories);

    return geminiTextClient.generate(prompt)
        .filter(this::isValidResponse)
        .map(this::parseResponseDto)
        .orElseThrow(() -> new RuntimeException(
            "Failed to generate seasonal recommendations from AI."));
  }

  public boolean isValidResponse(GeminiTextResponse response) {
    return response != null && response.text() != null && !response.text().isBlank();
  }

  private RecommendationResponseDTO parseResponseDto(GeminiTextResponse response) {
    try {
      return objectMapper.readValue(response.text(), RecommendationResponseDTO.class);
    } catch (Exception exception) {
      LOGGER.error(
          "Failed to parse AI JSON response into DTO. Raw text: {}",
          response.text(),
          exception);
      throw new RuntimeException("Data parsing error from AI response", exception);
    }
  }

  private String buildPrompt(
      String month,
      Double temperature,
      String weatherCondition,
      String availableCategories) {
    return """
        ROL:
        Eres un sistema experto en retail y análisis comercial para kioscos escolares.
        Tu objetivo es procesar las condiciones climáticas y la época del año para sugerir
        ajustes en las compras de mercadería.

        CONTEXTO ACTUAL:
        - Fecha actual: %s
        - Clima actual: %s°C, %s
        - Tipo de establecimiento: Kiosco en escuela Primaria (solo venta de alimentos y bebidas).
        - Categorías disponibles: %s
        
        REGLAS DE NEGOCIO:
        1. Evalúa el contexto y selecciona productos estrictamente de las
           categorías disponibles mencionadas arriba.
        2. Identifica 2 categorías cuyo stock se deba AUMENTAR por el clima/época.
        3. Identifica 1 categoría cuyo stock se deba REDUCIR por el clima/época.
        4. Las justificaciones deben ser comerciales, breves (máximo 15 palabras) y lógicas.

        FORMATO DE SALIDA ESTRICTO:
        No escribas saludos, introducciones ni texto adicional.
        Tu única respuesta debe ser un objeto JSON válido con la siguiente estructura exacta:

        {
          "sugerencias": [
            {
              "categoria": "Nombre de la categoría (debe existir en las disponibles)",
              "accion": "AUMENTAR o REDUCIR",
              "motivo": "Justificación comercial breve.",
              "prioridad": "Alta, Media o Baja",
              "icono": "emoji representativo, ej: 🍦, 🥤, 🍫"
            }
          ],
          "tip_promocional": "Idea breve de promo cruzada.",
          "promocion_sugerida": {
            "nombre": "Nombre comercial atractivo para la promoción",
            "descuento": 15.0,
            "categorias_destino": ["Nombre Categoria 1", "Nombre Categoria 2"],
            "imagen": "URL de imagen placeholder (ej. https://via.placeholder.com/150) o un emoji muy descriptivo."
          }
        }
        """
        .formatted(month, temperature, weatherCondition, availableCategories);
  }

}
