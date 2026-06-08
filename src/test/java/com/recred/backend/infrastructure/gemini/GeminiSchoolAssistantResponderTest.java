package com.recred.backend.infrastructure.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.EstadoPagoExterno;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantDraft;
import com.recred.backend.domain.models.SchoolAssistantPayment;
import com.recred.backend.domain.models.SchoolAssistantPrompt;
import com.recred.backend.domain.models.SchoolAssistantPurchase;
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
import static org.mockito.Mockito.verify;
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
        verify(geminiTextClient).generate(contains(
                "Solo podes responder sobre estas capacidades: [SALDO, PAGOS, COMPRAS, EVENTOS]."
        ));
        verify(geminiTextClient).generate(contains(
                "Si el usuario pregunta otra cosa, responde exactamente:"
        ));
        verify(geminiTextClient).generate(contains(
                "Si enumeras 3 o mas elementos, usa una linea introductoria y vinetas con \"- \"."
        ));
        verify(geminiTextClient).generate(contains("No separes elementos con punto y coma."));
        verify(geminiTextClient).generate(contains("comprasRecientes=[320.00|ENTREGADO|CREDITOS|"));
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
                new SchoolAssistantContext(
                        UUID.randomUUID(),
                        Optional.of(new BigDecimal("500.00")),
                        List.of(new SchoolAssistantPayment(
                                new BigDecimal("1000.00"),
                                EstadoPagoExterno.APROBADO,
                                LocalDateTime.of(2026, 5, 26, 10, 0),
                                LocalDateTime.of(2026, 5, 26, 10, 5)
                        )),
                        List.of(new SchoolAssistantPurchase(
                                new BigDecimal("320.00"),
                                EstadoCompra.ENTREGADO,
                                MedioPago.CREDITOS,
                                LocalDateTime.of(2026, 5, 27, 9, 0)
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
