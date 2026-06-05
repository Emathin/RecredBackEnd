package com.recred.backend.infrastructure.gemini;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.SchoolAssistantDraft;
import com.recred.backend.domain.models.SchoolAssistantPayment;
import com.recred.backend.domain.models.SchoolAssistantPrompt;
import com.recred.backend.domain.models.SchoolAssistantPurchase;
import com.recred.backend.domain.models.SchoolCalendarEvent;
import com.recred.backend.domain.services.SchoolAssistantResponder;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GeminiSchoolAssistantResponder implements SchoolAssistantResponder {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            GeminiSchoolAssistantResponder.class);
    private static final String PROMPT_TEMPLATE = """
            Sos un asistente virtual escolar de Recred.
            Atendes a un usuario con rol %s.
            Solo podes responder sobre estas capacidades: %s.
            Usa solamente los datos provistos en la seccion Datos.
            Si un dato no esta presente, deci que no hay informacion registrada.
            No inventes saldos, pagos, compras, fechas, eventos, stock ni pedidos.
            Si el usuario pregunta otra cosa, responde exactamente:
            "No puedo ayudarte con ese tema ahora mismo."
            No ofrezcas hacer pagos, cancelar compras ni modificar datos.
            Responde en espanol rioplatense, claro y breve.
            La respuesta debe tener como maximo 2 oraciones.
            Responde solo JSON valido con esta forma:
            {
              "respuesta": "texto para usuario final"
            }

            Consulta:
            %s

            Datos:
            rol=%s
            usuarioId=%s
            alumnoId=%s
            buffetId=%s
            saldo=%s
            pagosRecientes=%s
            comprasRecientes=%s
            eventosProximos=%s
            datosAdicionales=%s
            historial=%s
            """;

    private final GeminiTextClient geminiTextClient;
    private final ObjectMapper objectMapper;

    public GeminiSchoolAssistantResponder(
            GeminiTextClient geminiTextClient,
            ObjectMapper objectMapper
    ) {
        this.geminiTextClient = geminiTextClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<SchoolAssistantDraft> respond(SchoolAssistantPrompt prompt) {
        return geminiTextClient.generate(prompt(prompt))
                .flatMap(this::parseResponse);
    }

    private Optional<SchoolAssistantDraft> parseResponse(GeminiTextResponse response) {
        try {
            GeminiAssistantResponse payload = objectMapper.readValue(
                    response.text(),
                    GeminiAssistantResponse.class
            );
            return Optional.ofNullable(payload.answer())
                    .filter(answer -> !answer.isBlank())
                    .map(answer -> new SchoolAssistantDraft(answer, response.model()));
        } catch (JsonProcessingException exception) {
            LOGGER.warn(
                    "Gemini returned invalid school assistant response: {}",
                    exception.getOriginalMessage()
            );
            return Optional.empty();
        }
    }

    private String prompt(SchoolAssistantPrompt prompt) {
        return PROMPT_TEMPLATE.formatted(
                prompt.context().role(),
                prompt.context().capabilities(),
                prompt.question(),
                prompt.context().role(),
                prompt.context().userId(),
                prompt.context().studentId(),
                prompt.context().buffetId(),
                prompt.context().balance().map(Object::toString).orElse("sin_datos"),
                payments(prompt.context().recentPayments()),
                purchases(prompt.context().recentPurchases()),
                events(prompt.context().upcomingEvents()),
                prompt.context().facts(),
                history(prompt.recentMessages())
        );
    }

    private List<String> payments(List<SchoolAssistantPayment> payments) {
        return payments.stream()
                .map(payment -> payment.amount()
                        + "|"
                        + payment.status()
                        + "|"
                        + payment.createdAt()
                        + "|"
                        + payment.approvedAt())
                .toList();
    }

    private List<String> purchases(List<SchoolAssistantPurchase> purchases) {
        return purchases.stream()
                .map(purchase -> purchase.totalAmount()
                        + "|"
                        + purchase.status()
                        + "|"
                        + purchase.paymentMethod()
                        + "|"
                        + purchase.dateTime())
                .toList();
    }

    private List<String> events(List<SchoolCalendarEvent> events) {
        return events.stream()
                .map(event -> event.date()
                        + "|"
                        + event.type()
                        + "|"
                        + event.description())
                .toList();
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

    private record GeminiAssistantResponse(
            @JsonProperty("respuesta")
            String answer
    ) {
    }
}
