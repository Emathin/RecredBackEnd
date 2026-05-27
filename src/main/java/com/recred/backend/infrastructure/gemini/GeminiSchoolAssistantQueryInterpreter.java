package com.recred.backend.infrastructure.gemini;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.DateRange;
import com.recred.backend.domain.models.SchoolAssistantCapability;
import com.recred.backend.domain.models.SchoolAssistantQuery;
import com.recred.backend.domain.services.SchoolAssistantAiQueryInterpreter;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GeminiSchoolAssistantQueryInterpreter implements SchoolAssistantAiQueryInterpreter {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            GeminiSchoolAssistantQueryInterpreter.class);
    private static final int DEFAULT_PAYMENT_LIMIT = 5;
    private static final int MIN_PAYMENT_LIMIT = 1;
    private static final int MAX_PAYMENT_LIMIT = 10;
    private static final long MAX_PAYMENT_RANGE_DAYS = 366;
    private static final long MAX_EVENT_RANGE_DAYS = 120;
    private static final String PROMPT_TEMPLATE = """
            Sos un interprete de consultas para un asistente escolar.
            Tu unica tarea es convertir el mensaje en un plan JSON de consulta.
            Capacidades disponibles: SALDO, PAGOS y EVENTOS.
            No respondas al usuario final.
            Usa fechas absolutas ISO-8601 tomando fechaActual como referencia.
            Si el usuario dice "hoy", "manana", "esta semana", "mayo" o similares,
            calcula desde y hasta.
            Si la consulta depende del historial, usalo para resolver referencias como
            "y la semana que viene", "los anteriores" o "entonces".
            Si no corresponde a saldo, pagos o eventos, marca fueraDeAlcance=true.
            Responde solo JSON valido con esta forma:
            {
              "capacidades": ["SALDO", "PAGOS", "EVENTOS"],
              "rangoPagos": {"desde": "2026-05-01", "hasta": "2026-05-31"},
              "rangoEventos": {"desde": "2026-05-27", "hasta": "2026-06-03"},
              "limitePagos": 5,
              "requiereRedaccionIa": true,
              "fueraDeAlcance": false
            }

            fechaActual=%s
            historial=%s
            mensaje=%s
            """;

    private final GeminiTextClient geminiTextClient;
    private final ObjectMapper objectMapper;

    public GeminiSchoolAssistantQueryInterpreter(
            GeminiTextClient geminiTextClient,
            ObjectMapper objectMapper
    ) {
        this.geminiTextClient = geminiTextClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<SchoolAssistantQuery> interpret(
            String question,
            LocalDate currentDate,
            List<ChatMessage> recentMessages
    ) {
        return geminiTextClient.generate(prompt(question, currentDate, recentMessages))
                .flatMap(this::parseResponse);
    }

    private Optional<SchoolAssistantQuery> parseResponse(GeminiTextResponse response) {
        try {
            GeminiQueryResponse payload = objectMapper.readValue(
                    response.text(),
                    GeminiQueryResponse.class
            );
            return Optional.of(query(payload));
        } catch (JsonProcessingException exception) {
            LOGGER.warn(
                    "Gemini returned invalid school assistant query: {}",
                    exception.getOriginalMessage()
            );
            return Optional.empty();
        }
    }

    private SchoolAssistantQuery query(GeminiQueryResponse payload) {
        List<SchoolAssistantCapability> capabilities = capabilities(payload);

        if (capabilities.contains(SchoolAssistantCapability.FUERA_DE_ALCANCE)) {
            return outOfScopeQuery();
        }

        return new SchoolAssistantQuery(
                capabilities,
                dateRange(payload.paymentDateRange(), MAX_PAYMENT_RANGE_DAYS),
                dateRange(payload.eventDateRange(), MAX_EVENT_RANGE_DAYS),
                paymentLimit(payload.paymentLimit()),
                requiresAiResponse(payload.requiresAiResponse(), capabilities)
        );
    }

    private List<SchoolAssistantCapability> capabilities(GeminiQueryResponse payload) {
        if (Boolean.TRUE.equals(payload.outOfScope())) {
            return List.of(SchoolAssistantCapability.FUERA_DE_ALCANCE);
        }

        List<SchoolAssistantCapability> capabilities = normalizeCapabilities(payload.names());
        return capabilities.isEmpty()
                ? List.of(SchoolAssistantCapability.FUERA_DE_ALCANCE)
                : capabilities;
    }

    private List<SchoolAssistantCapability> normalizeCapabilities(List<String> capabilities) {
        if (capabilities == null) {
            return List.of();
        }

        return capabilities.stream()
                .map(this::capability)
                .flatMap(Optional::stream)
                .filter(capability -> capability != SchoolAssistantCapability.FUERA_DE_ALCANCE)
                .distinct()
                .toList();
    }

    private Optional<SchoolAssistantCapability> capability(String value) {
        try {
            return Optional.of(SchoolAssistantCapability.valueOf(value));
        } catch (IllegalArgumentException | NullPointerException exception) {
            return Optional.empty();
        }
    }

    private Optional<DateRange> dateRange(DateRangePayload payload, long maxRangeDays) {
        if (payload == null || payload.from() == null || payload.to() == null) {
            return Optional.empty();
        }

        try {
            LocalDate startDate = LocalDate.parse(payload.from());
            LocalDate endDate = LocalDate.parse(payload.to());

            if (endDate.isBefore(startDate)) {
                return Optional.empty();
            }

            return Optional.of(new DateRange(
                    startDate,
                    limitedEndDate(startDate, endDate, maxRangeDays)
            ));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private LocalDate limitedEndDate(
            LocalDate startDate,
            LocalDate endDate,
            long maxRangeDays
    ) {
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        return days > maxRangeDays ? startDate.plusDays(maxRangeDays) : endDate;
    }

    private int paymentLimit(Integer paymentLimit) {
        if (paymentLimit == null) {
            return DEFAULT_PAYMENT_LIMIT;
        }

        return Math.max(MIN_PAYMENT_LIMIT, Math.min(MAX_PAYMENT_LIMIT, paymentLimit));
    }

    private boolean requiresAiResponse(
            Boolean requiresAiResponse,
            List<SchoolAssistantCapability> capabilities
    ) {
        return requiresAiResponse != null ? requiresAiResponse : capabilities.size() > 1;
    }

    private SchoolAssistantQuery outOfScopeQuery() {
        return new SchoolAssistantQuery(
                List.of(SchoolAssistantCapability.FUERA_DE_ALCANCE),
                Optional.empty(),
                Optional.empty(),
                DEFAULT_PAYMENT_LIMIT,
                false
        );
    }

    private String prompt(
            String question,
            LocalDate currentDate,
            List<ChatMessage> recentMessages
    ) {
        return PROMPT_TEMPLATE.formatted(
                currentDate,
                history(recentMessages),
                question
        );
    }

    private List<String> history(List<ChatMessage> recentMessages) {
        return recentMessages.stream()
                .map(message -> message.role()
                        + "|"
                        + message.dateTime()
                        + "|"
                        + message.content())
                .toList();
    }

    private record GeminiQueryResponse(
            @JsonProperty("capacidades")
            List<String> capabilities,
            @JsonProperty("intenciones")
            List<String> legacyIntents,
            @JsonProperty("rangoPagos")
            DateRangePayload paymentDateRange,
            @JsonProperty("rangoEventos")
            DateRangePayload eventDateRange,
            @JsonProperty("limitePagos")
            Integer paymentLimit,
            @JsonProperty("requiereRedaccionIa")
            Boolean requiresAiResponse,
            @JsonProperty("fueraDeAlcance")
            Boolean outOfScope
    ) {

        List<String> names() {
            return capabilities == null ? legacyIntents : capabilities;
        }
    }

    private record DateRangePayload(
            @JsonProperty("desde")
            String from,
            @JsonProperty("hasta")
            String to
    ) {
    }
}
