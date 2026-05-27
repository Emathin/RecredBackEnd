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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FlexibleSchoolAssistantQueryInterpreterTest {

    private final SchoolAssistantAiQueryInterpreter aiInterpreter = mock(
            SchoolAssistantAiQueryInterpreter.class
    );
    private final FlexibleSchoolAssistantQueryInterpreter interpreter =
            new FlexibleSchoolAssistantQueryInterpreter(
                    new SchoolAssistantLocalQueryInterpreter(),
                    aiInterpreter
            );
    private final LocalDate currentDate = LocalDate.of(2026, 5, 27);

    @Test
    void useLocalInterpreterWhenQuestionIsClear() {
        Optional<SchoolAssistantQuery> query = interpreter.interpret(
                "saldo",
                currentDate,
                List.of()
        );

        assertTrue(query.isPresent());
        assertEquals(List.of(SchoolAssistantCapability.SALDO), query.orElseThrow()
                .capabilities());
        verifyNoInteractions(aiInterpreter);
    }

    @Test
    void useAiInterpreterWhenQuestionDependsOnHistory() {
        List<ChatMessage> history = history();
        SchoolAssistantQuery aiQuery = new SchoolAssistantQuery(
                List.of(SchoolAssistantCapability.EVENTOS),
                Optional.empty(),
                Optional.empty(),
                5,
                true
        );

        when(aiInterpreter.interpret("y la semana que viene", currentDate, history))
                .thenReturn(Optional.of(aiQuery));

        Optional<SchoolAssistantQuery> query = interpreter.interpret(
                "y la semana que viene",
                currentDate,
                history
        );

        assertEquals(aiQuery, query.orElseThrow());
    }

    @Test
    void fallbackToOutOfScopeWhenAiDoesNotResolve() {
        when(aiInterpreter.interpret("consulta rara", currentDate, List.of()))
                .thenReturn(Optional.empty());

        Optional<SchoolAssistantQuery> query = interpreter.interpret(
                "consulta rara",
                currentDate,
                List.of()
        );

        assertTrue(query.isPresent());
        assertEquals(
                List.of(SchoolAssistantCapability.FUERA_DE_ALCANCE),
                query.orElseThrow().capabilities()
        );
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
