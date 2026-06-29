package com.recred.backend.infrastructure.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.LoadStockIARequest;
import com.recred.backend.application.LoadStockIAResponse;
import com.recred.backend.domain.services.LoadStockAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;


public class LoadStockIAService implements LoadStockAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadStockIAService.class);
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final List<String> ALLOWED_FORMATS =
            List.of("image/jpeg", "image/png", "image/jpg");
    private static final String PROMPT_TEMPLATE = """
            Sos un OCR de imagenes de una app escolar.
            Analiza la siguiente imagen.
            Usa lenguaje simple y claro.
            No me retornes nada mas que el JSON que te pido con los siguientes datos,\s
            si no tiene datos solo coloca este caracter "-"
            {
              "nombre": "texto breve",
              "descripcion": "texto breve",
              "peso": "texto breve",
              "contiene_azucar": "true o false",
              "contiene_mani": "true o false",
              "contiene_lactosa": "true o false",
              "contiene_tacc":"true o false"
            }
            """;

    private final GeminiTextClient geminiTextClient;
    private final ObjectMapper objectMapper;

    public LoadStockIAService(GeminiTextClient geminiTextClient, ObjectMapper objectMapper) {
        this.geminiTextClient = geminiTextClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public LoadStockIAResponse analyze(LoadStockIARequest request) {
        validarFormato(request);
        LoadStockIARequest imagenSanitizada = sanitizarImagen(request);
        return consultarIA(imagenSanitizada);
    }

    private void validarFormato(LoadStockIARequest request) {
        if (request.fileBytes() == null || request.fileBytes().length == 0) {
            throw new IllegalArgumentException("El archivo no puede estar vacío");
        }

        String contentType = request.contentType();
        if (contentType == null || !ALLOWED_FORMATS.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Formato de imagen no permitido. Solo se aceptan: " + ALLOWED_FORMATS);
        }

        if (request.fileSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("El archivo excede el tamaño máximo de 5 MB");
        }
    }

    private LoadStockIARequest sanitizarImagen(LoadStockIARequest request) {
        return request;
    }

    private LoadStockIAResponse consultarIA(LoadStockIARequest request) {
        try {
            Optional<GeminiTextResponse> response = geminiTextClient
                    .generateWithFile(PROMPT_TEMPLATE, request.fileBytes(), request.contentType());

            if (response.isEmpty()) {
                LOGGER.warn("La IA no generó una respuesta");
                return crearRespuestaVacia();
            }

            GeminiTextResponse geminiResponse = response.get();
            return parsearRespuestaGemini(geminiResponse.text());

        } catch (Exception e) {
            LOGGER.error("Error al consultar la IA: {}", e.getMessage(), e);
            throw new RuntimeException("Error procesando la imagen con IA", e);
        }
    }


    private LoadStockIAResponse parsearRespuestaGemini(String jsonContent) {
        try {
            LoadStockAnalysisResponse analysisResponse = objectMapper.readValue(
                    jsonContent,
                    LoadStockAnalysisResponse.class
            );

            return new LoadStockIAResponse(
                    analysisResponse.nombre,
                    analysisResponse.descripcion,
                    analysisResponse.peso,
                    analysisResponse.contieneAzucar,
                    analysisResponse.contieneMani,
                    analysisResponse.contieneLactosa,
                    analysisResponse.contieneTacc,
                    null
            );

        } catch (IOException e) {
            LOGGER.error("Error al parsear la respuesta JSON de Gemini: {}", e.getMessage(), e);
            return crearRespuestaVacia();
        }
    }


    private LoadStockIAResponse crearRespuestaVacia() {
        return new LoadStockIAResponse(
                "-",
                "-",
                "-",
                "-",
                "-",
                "-",
                "-",
                null
        );
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LoadStockAnalysisResponse {
        @JsonProperty("nombre")
        public String nombre;

        @JsonProperty("descripcion")
        public String descripcion;

        @JsonProperty("peso")
        public String peso;

        @JsonProperty("contiene_azucar")
        public String contieneAzucar;

        @JsonProperty("contiene_mani")
        public String contieneMani;

        @JsonProperty("contiene_lactosa")
        public String contieneLactosa;

        @JsonProperty("contiene_tacc")
        public String contieneTacc;
    }
}
