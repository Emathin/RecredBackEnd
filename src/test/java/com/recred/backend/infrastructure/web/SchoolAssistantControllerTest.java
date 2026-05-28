package com.recred.backend.infrastructure.web;

import com.recred.backend.application.AskSchoolAssistant;
import com.recred.backend.application.SchoolAssistantCommand;
import com.recred.backend.domain.models.SchoolAssistantAnswer;
import com.recred.backend.domain.models.SchoolAssistantCapability;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SchoolAssistantControllerTest {

    private AskSchoolAssistant askSchoolAssistant;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        askSchoolAssistant = mock(AskSchoolAssistant.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SchoolAssistantController(askSchoolAssistant))
                .build();
    }

    @Test
    void askDelegatesMessageToUseCase() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        LocalDateTime dateTime = LocalDateTime.of(2026, 5, 27, 12, 0);
        SchoolAssistantAnswer answer = new SchoolAssistantAnswer(
                sessionId,
                "Tenes $500.00 disponibles.",
                List.of(SchoolAssistantCapability.SALDO),
                dateTime,
                true,
                "gemini-test"
        );
        when(askSchoolAssistant.execute(eq(studentId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(answer);

        mockMvc.perform(post("/ia/alumnos/{alumnoId}/asistente/mensajes", studentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sesionId": "%s",
                                  "mensaje": "saldo"
                                }
                                """.formatted(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sesionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.respuesta").value("Tenes $500.00 disponibles."))
                .andExpect(jsonPath("$.capacidades[0]").value("SALDO"))
                .andExpect(jsonPath("$.generadoPorIa").value(true))
                .andExpect(jsonPath("$.modelo").value("gemini-test"));

        ArgumentCaptor<SchoolAssistantCommand> commandCaptor = ArgumentCaptor
                .forClass(SchoolAssistantCommand.class);
        verify(askSchoolAssistant).execute(eq(studentId), commandCaptor.capture());
        SchoolAssistantCommand command = commandCaptor.getValue();
        assertEquals("saldo", command.message());
        assertEquals(Optional.of(sessionId), command.sessionId());
        assertTrue(command.sessionId().isPresent());
    }
}
