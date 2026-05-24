package com.recred.backend.infrastructure.gemini;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.AnalisisPrediccionGasto;
import com.recred.backend.domain.models.CategoriaConsumo;
import com.recred.backend.domain.models.PrediccionGasto;
import com.recred.backend.domain.services.AnalizadorPrediccionGasto;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GeminiAnalizadorPrediccionGasto implements AnalizadorPrediccionGasto {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            GeminiAnalizadorPrediccionGasto.class);

    private final GeminiTextClient geminiTextClient;
    private final ObjectMapper objectMapper;

    public GeminiAnalizadorPrediccionGasto(
            GeminiTextClient geminiTextClient,
            ObjectMapper objectMapper
    ) {
        this.geminiTextClient = geminiTextClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<AnalisisPrediccionGasto> analizar(PrediccionGasto prediccionGasto) {
        return geminiTextClient.generate(prompt(prediccionGasto))
                .flatMap(this::parsearRespuesta);
    }

    private Optional<AnalisisPrediccionGasto> parsearRespuesta(GeminiTextResponse response) {
        try {
            GeminiAnalisisResponse payload = objectMapper.readValue(
                    response.text(),
                    GeminiAnalisisResponse.class
            );
            return Optional.of(new AnalisisPrediccionGasto(
                    payload.resumen(),
                    normalizarLista(payload.alertas()),
                    normalizarLista(payload.recomendaciones()),
                    response.model()
            ));
        } catch (JsonProcessingException exception) {
            LOGGER.warn(
                    "Gemini devolvio un analisis de prediccion invalido: {}",
                    exception.getOriginalMessage()
            );
            return Optional.empty();
        }
    }

    private String prompt(PrediccionGasto prediccionGasto) {
        return """
                Analiza una prediccion de gasto escolar sin recalcular el monto.
                Responde solo JSON valido con esta forma:
                {
                  "resumen": "texto breve",
                  "alertas": ["texto"],
                  "recomendaciones": ["texto"]
                }

                Datos:
                periodo=%s
                fechaCalculo=%s
                fechaInicio=%s
                fechaFin=%s
                gastoActual=%s
                gastoPredicho=%s
                promedioGastoDiario=%s
                montoLimite=%s
                porcentajePresupuesto=%s
                confianza=%s
                diasHistoricosUsados=%s
                diasRestantes=%s
                categoriasMasConsumidas=%s
                """.formatted(
                prediccionGasto.periodo(),
                prediccionGasto.fechaCalculo(),
                prediccionGasto.fechaInicio(),
                prediccionGasto.fechaFin(),
                prediccionGasto.gastoActual(),
                prediccionGasto.gastoPredicho(),
                prediccionGasto.promedioGastoDiario(),
                prediccionGasto.montoLimite(),
                prediccionGasto.porcentajePresupuesto(),
                prediccionGasto.confianza(),
                prediccionGasto.diasHistoricosUsados(),
                prediccionGasto.diasRestantes(),
                categorias(prediccionGasto.categoriasMasConsumidas())
        );
    }

    private String categorias(List<CategoriaConsumo> categorias) {
        return categorias.stream()
                .map(categoria -> categoria.descripcion() + "=" + categoria.montoTotal())
                .toList()
                .toString();
    }

    private List<String> normalizarLista(List<String> valores) {
        if (valores == null) {
            return List.of();
        }
        return valores.stream()
                .filter(valor -> valor != null && !valor.isBlank())
                .toList();
    }

    private record GeminiAnalisisResponse(
            String resumen,
            List<String> alertas,
            List<String> recomendaciones
    ) {
    }
}
