package com.recred.backend.infrastructure.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.RawSupplierPriceItem;
import com.recred.backend.domain.services.SupplierPriceListAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GeminiSupplierPriceListAnalyzer implements SupplierPriceListAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            GeminiSupplierPriceListAnalyzer.class);

    private static final String SYSTEM_PROMPT = """
            Sos un experto en extracción de datos de listas de precios de proveedores.
            Analizá el documento adjunto (PDF o CSV) y extraé TODOS los productos con su precio.
            
            Para cada producto generá un objeto JSON con:
            - nombre_producto: Nombre del artículo tal como aparece en la lista.
            - precio: Número decimal (precio unitario o por la unidad indicada).
            - unidad: Descripción de la unidad (ej: "unidad", "caja x 12", "pack x 6").
              Si no se especifica, usa "unidad".
            - notas: Cualquier observación relevante (descuento, condición, vencimiento).
              Si no hay, null.
            
            REGLAS:
            1. Retorná ÚNICAMENTE un JSON con la estructura {"items": [...]}.
            2. No inventes datos. Si un campo no está presente, usá null.
            3. Respondé SIEMPRE en español.
            """;

    private final GeminiTextClient geminiTextClient;
    private final ObjectMapper objectMapper;

    public GeminiSupplierPriceListAnalyzer(
            GeminiTextClient geminiTextClient,
            ObjectMapper objectMapper
    ) {
        this.geminiTextClient = geminiTextClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<RawSupplierPriceItem> analyze(byte[] fileContent, String contentType) {
        try {
            return geminiTextClient.generateWithFile(SYSTEM_PROMPT, fileContent, contentType)
                    .map(response -> {
                        LOGGER.info("Raw Gemini Price List Response: {}", response.text());
                        return parseResponse(response.text());
                    })
                    .orElse(Collections.emptyList());
        } catch (Exception e) {
            LOGGER.error("Error analizando lista de precios con Gemini: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<RawSupplierPriceItem> parseResponse(String json) {
        try {
            String cleanedJson = cleanJson(json);
            GeminiPriceListResponse response = objectMapper.readValue(
                    cleanedJson, GeminiPriceListResponse.class);
            if (response.items == null) {
                LOGGER.warn("Gemini returned null items for price list. Cleaned JSON: {}",
                        cleanedJson);
                return Collections.emptyList();
            }
            return response.items.stream()
                    .map(this::mapToRaw)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("Error parseando JSON de lista de precios de Gemini: {}. "
                    + "Raw JSON: {}", e.getMessage(), json);
            return java.util.Collections.emptyList();
        }
    }

    private String cleanJson(String json) {
        if (json == null) {
            return null;
        }
        return json.trim()
                .replace("```json", "")
                .replace("```", "")
                .trim();
    }

    private RawSupplierPriceItem mapToRaw(GeminiRawItem item) {
        return new RawSupplierPriceItem(
                item.nombreProducto,
                item.precio,
                item.unidad,
                item.notas
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GeminiPriceListResponse {
        public List<GeminiRawItem> items;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GeminiRawItem {
        @JsonProperty("nombre_producto")
        public String nombreProducto;
        public java.math.BigDecimal precio;
        public String unidad;
        public String notas;
    }
}
