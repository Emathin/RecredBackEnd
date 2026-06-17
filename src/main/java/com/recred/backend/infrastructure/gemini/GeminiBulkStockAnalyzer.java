package com.recred.backend.infrastructure.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.RawBulkProduct;
import com.recred.backend.domain.services.BulkStockAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GeminiBulkStockAnalyzer implements BulkStockAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeminiBulkStockAnalyzer.class);
    
    private static final String SYSTEM_PROMPT_TEMPLATE = """
            Sos un experto en extracción de datos de inventario. Tu tarea es analizar el documento
            adjunto (PDF o imagen) y extraer TODOS los productos de la lista.
            
            Categorías permitidas (usar nombre exacto): [%s]

            Para cada producto, genera un objeto con:
            - nombre: Nombre del producto.
            - descripcion: Detalle corto.
            - precio: Número decimal.
            - peso: Número decimal (en gramos o ml).
            - requiere_preparacion: Boolean (true si es comida elaborada, false si es industrial).
            - categoria_nombre: El nombre exacto de la categoría de la lista anterior.
            - stock_actual: Número entero.
            - salud_etiquetas: Array de strings (ej: ["Sin TACC", "Sin Azúcar"]).
            - tipo_etiquetas: Array de strings (ej: ["Lácteo", "Bebida"]).

            REGLAS CRÍTICAS:
            1. Retorna ÚNICAMENTE un JSON con la estructura {"productos": [...]}.
            2. Si la lista es muy larga, procésala completa hasta el final sin cortar el JSON.
            3. Para 'categoria_nombre', intentá usar uno de los valores de la lista de categorías. 
               Si el producto no encaja claramente en ninguna, sugerí un nombre de categoría
               nuevo, corto y descriptivo (ej: 'Snacks', 'Bebidas'). NO devuelvas null.
            4. No inventes otros datos. Si un campo no está, usa null.
            5. Responde SIEMPRE en español.
            """;

    private final GeminiTextClient geminiTextClient;
    private final ObjectMapper objectMapper;

    public GeminiBulkStockAnalyzer(GeminiTextClient geminiTextClient, ObjectMapper objectMapper) {
        this.geminiTextClient = geminiTextClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<RawBulkProduct> analyze(byte[] fileContent,
                                        String contentType,
                                        List<String> availableCategories) {
        try {
            String categoriesList = String.join("|", availableCategories);
            String prompt = String.format(SYSTEM_PROMPT_TEMPLATE, categoriesList);

            return geminiTextClient.generateWithFile(prompt, fileContent, contentType)
                    .map(response -> {
                        LOGGER.info("Raw Gemini Response: {}", response.text());
                        return parseResponse(response.text());
                    })
                    .orElse(Collections.emptyList());
        } catch (Exception e) {
            LOGGER.error("Error analizando archivo con Gemini: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<RawBulkProduct> parseResponse(String json) {
        try {
            String cleanedJson = cleanJson(json);
            GeminiBulkResponse response = objectMapper
                    .readValue(cleanedJson, GeminiBulkResponse.class);
            if (response.productos == null) {
                LOGGER.warn("Gemini returned a null product list. Cleaned JSON: {}", cleanedJson);
                return Collections.emptyList();
            }
            return response.productos.stream()
                    .map(this::mapToRaw)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("Error parseando JSON de Gemini: {}. Raw JSON: {}",
                    e.getMessage(), json);
            return Collections.emptyList();
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

    private RawBulkProduct mapToRaw(RawProduct raw) {
        return new RawBulkProduct(
                raw.nombre,
                raw.descripcion,
                raw.precio,
                raw.peso,
                raw.requierePreparacion,
                raw.categoriaNombre,
                raw.stockActual,
                raw.saludEtiquetas != null ? raw.saludEtiquetas : Collections.emptyList(),
                raw.tipoEtiquetas != null ? raw.tipoEtiquetas : Collections.emptyList()
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GeminiBulkResponse {
        public List<RawProduct> productos;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RawProduct {
        public String nombre;
        public String descripcion;
        public java.math.BigDecimal precio;
        public java.math.BigDecimal peso;
        @JsonProperty("requiere_preparacion")
        public boolean requierePreparacion;
        @JsonProperty("categoria_nombre")
        public String categoriaNombre;
        @JsonProperty("stock_actual")
        public Integer stockActual;
        @JsonProperty("salud_etiquetas")
        public List<String> saludEtiquetas;
        @JsonProperty("tipo_etiquetas")
        public List<String> tipoEtiquetas;
    }
}
