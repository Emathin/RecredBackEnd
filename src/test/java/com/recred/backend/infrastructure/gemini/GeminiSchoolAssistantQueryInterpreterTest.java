package com.recred.backend.infrastructure.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.RolChatMensaje;
import com.recred.backend.domain.models.SchoolAssistantCapability;
import com.recred.backend.domain.models.SchoolAssistantQuery;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeminiSchoolAssistantQueryInterpreterTest {

    private final GeminiTextClient geminiTextClient = mock(GeminiTextClient.class);
    private final GeminiSchoolAssistantQueryInterpreter interpreter =
            new GeminiSchoolAssistantQueryInterpreter(geminiTextClient, new ObjectMapper());

    @Test
    void parseGeminiQueryJsonResponse() {
        when(geminiTextClient.generate(contains("mensaje=pagos de mayo"))).thenReturn(
                Optional.of(new GeminiTextResponse("""
                        {
                          "capacidades": ["PAGOS", "EVENTOS"],
                          "rangoPagos": {
                            "desde": "2026-05-01",
                            "hasta": "2026-05-31"
                          },
                          "rangoEventos": {
                            "desde": "2026-06-01",
                            "hasta": "2026-06-07"
                          },
                          "limitePagos": 20,
                          "requiereRedaccionIa": true,
                          "fueraDeAlcance": false
                        }
                        """, "gemini-test"))
        );

        Optional<SchoolAssistantQuery> query = interpreter.interpret(
                "pagos de mayo",
                LocalDate.of(2026, 5, 27),
                history()
        );

        assertTrue(query.isPresent());
        assertEquals(List.of(
                SchoolAssistantCapability.PAGOS,
                SchoolAssistantCapability.EVENTOS
        ), query.orElseThrow().capabilities());
        assertEquals(10, query.orElseThrow().paymentLimit());
        assertTrue(query.orElseThrow().requiresAiResponse());
        assertEquals(
                LocalDate.of(2026, 5, 1),
                query.orElseThrow().paymentDateRange().orElseThrow().startDate()
        );
        assertEquals(
                LocalDate.of(2026, 6, 7),
                query.orElseThrow().eventDateRange().orElseThrow().endDate()
        );
    }

    @Test
    void returnOutOfScopeWhenGeminiMarksIt() {
        when(geminiTextClient.generate(contains("mensaje=contame un chiste"))).thenReturn(
                Optional.of(new GeminiTextResponse("""
                        {
                          "capacidades": [],
                          "limitePagos": 5,
                          "fueraDeAlcance": true
                        }
                        """, "gemini-test"))
        );

        Optional<SchoolAssistantQuery> query = interpreter.interpret(
                "contame un chiste",
                LocalDate.of(2026, 5, 27),
                List.of()
        );

        assertTrue(query.isPresent());
        assertEquals(
                List.of(SchoolAssistantCapability.FUERA_DE_ALCANCE),
                query.orElseThrow().capabilities()
        );
    }

    @Test
    void returnEmptyWhenGeminiReturnsInvalidJson() {
        when(geminiTextClient.generate(contains("mensaje=saldo"))).thenReturn(
                Optional.of(new GeminiTextResponse("no es json", "gemini-test"))
        );

        Optional<SchoolAssistantQuery> query = interpreter.interpret(
                "saldo",
                LocalDate.of(2026, 5, 27),
                List.of()
        );

        assertTrue(query.isEmpty());
    }

    private List<ChatMessage> history() {
        return List.of(new ChatMessage(
                UUID.randomUUID(),
                RolChatMensaje.USUARIO,
                "eventos de esta semana",
                null,
                LocalDateTime.of(2026, 5, 27, 11, 0)
        ));
    }
}
