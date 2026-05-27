package com.recred.backend.domain.services;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchoolAssistantLocalQueryInterpreterTest {

    private final SchoolAssistantLocalQueryInterpreter interpreter =
            new SchoolAssistantLocalQueryInterpreter();
    private final LocalDate currentDate = LocalDate.of(2026, 5, 27);

    @Test
    void interpretSimpleBalanceLocally() {
        Optional<SchoolAssistantQuery> query = interpreter.interpret(
                "saldo",
                currentDate,
                List.of()
        );

        assertTrue(query.isPresent());
        assertEquals(List.of(SchoolAssistantCapability.SALDO), query.orElseThrow()
                .capabilities());
        assertFalse(query.orElseThrow().requiresAiResponse());
    }

    @Test
    void interpretPaymentsWithMonthAndLimitLocally() {
        Optional<SchoolAssistantQuery> query = interpreter.interpret(
                "ultimos 3 pagos de mayo",
                currentDate,
                List.of()
        );

        assertTrue(query.isPresent());
        assertEquals(List.of(SchoolAssistantCapability.PAGOS), query.orElseThrow()
                .capabilities());
        assertEquals(3, query.orElseThrow().paymentLimit());
        assertEquals(
                LocalDate.of(2026, 5, 1),
                query.orElseThrow().paymentDateRange().orElseThrow().startDate()
        );
        assertEquals(
                LocalDate.of(2026, 5, 31),
                query.orElseThrow().paymentDateRange().orElseThrow().endDate()
        );
    }

    @Test
    void interpretNextWeekEventsLocally() {
        Optional<SchoolAssistantQuery> query = interpreter.interpret(
                "eventos semana que viene",
                currentDate,
                List.of()
        );

        assertTrue(query.isPresent());
        assertEquals(List.of(SchoolAssistantCapability.EVENTOS), query.orElseThrow()
                .capabilities());
        assertEquals(
                LocalDate.of(2026, 6, 1),
                query.orElseThrow().eventDateRange().orElseThrow().startDate()
        );
        assertEquals(
                LocalDate.of(2026, 6, 7),
                query.orElseThrow().eventDateRange().orElseThrow().endDate()
        );
    }

    @Test
    void requireAiWhenQuestionDependsOnHistory() {
        Optional<SchoolAssistantQuery> query = interpreter.interpret(
                "y la semana que viene",
                currentDate,
                history()
        );

        assertTrue(query.isEmpty());
    }

    @Test
    void requireAiResponseForMixedCapabilities() {
        Optional<SchoolAssistantQuery> query = interpreter.interpret(
                "saldo y eventos",
                currentDate,
                List.of()
        );

        assertTrue(query.isPresent());
        assertEquals(List.of(
                SchoolAssistantCapability.SALDO,
                SchoolAssistantCapability.EVENTOS
        ), query.orElseThrow().capabilities());
        assertTrue(query.orElseThrow().requiresAiResponse());
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
