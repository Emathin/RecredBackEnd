package com.recred.backend.infrastructure.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantCapability;
import com.recred.backend.domain.models.SchoolAssistantDraft;
import com.recred.backend.domain.models.SchoolAssistantPayment;
import com.recred.backend.domain.models.SchoolAssistantPrompt;
import com.recred.backend.domain.models.SchoolAssistantQuery;
import com.recred.backend.domain.models.SchoolCalendarEvent;
import com.recred.backend.domain.models.TipoDiaEscolar;
import java.math.BigDecimal;
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

class GeminiSchoolAssistantResponderTest {

    private final GeminiTextClient geminiTextClient = mock(GeminiTextClient.class);
    private final GeminiSchoolAssistantResponder responder =
            new GeminiSchoolAssistantResponder(geminiTextClient, new ObjectMapper());

    @Test
    void parseGeminiJsonResponse() {
        when(geminiTextClient.generate(contains("saldo=500.00"))).thenReturn(
                Optional.of(new GeminiTextResponse("""
                        {
                          "respuesta": "Tenes $500.00 disponibles."
                        }
                        """, "gemini-test"))
        );

        Optional<SchoolAssistantDraft> draft = responder.respond(prompt());

        assertTrue(draft.isPresent());
        assertEquals("Tenes $500.00 disponibles.", draft.orElseThrow().answer());
        assertEquals("gemini-test", draft.orElseThrow().model());
    }

    @Test
    void returnEmptyWhenGeminiReturnsInvalidJson() {
        when(geminiTextClient.generate(contains("saldo=500.00"))).thenReturn(
                Optional.of(new GeminiTextResponse("no es json", "gemini-test"))
        );

        Optional<SchoolAssistantDraft> draft = responder.respond(prompt());

        assertTrue(draft.isEmpty());
    }

    @Test
    void returnEmptyWhenGeminiDoesNotGenerateText() {
        when(geminiTextClient.generate(contains("saldo=500.00"))).thenReturn(
                Optional.empty()
        );

        Optional<SchoolAssistantDraft> draft = responder.respond(prompt());

        assertTrue(draft.isEmpty());
    }

    private SchoolAssistantPrompt prompt() {
        return new SchoolAssistantPrompt(
                "saldo",
                new SchoolAssistantQuery(
                        List.of(SchoolAssistantCapability.SALDO),
                        Optional.empty(),
                        Optional.empty(),
                        5,
                        true
                ),
                new SchoolAssistantContext(
                        UUID.randomUUID(),
                        Optional.of(new BigDecimal("500.00")),
                        List.of(new SchoolAssistantPayment(
                                new BigDecimal("1000.00"),
                                null,
                                LocalDateTime.of(2026, 5, 26, 10, 0),
                                null
                        )),
                        List.of(new SchoolCalendarEvent(
                                LocalDate.of(2026, 5, 30),
                                TipoDiaEscolar.CLASE,
                                "Clase normal"
                        ))
                ),
                List.of()
        );
    }
}
