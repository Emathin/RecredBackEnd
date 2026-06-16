package com.recred.backend.web;

import com.recred.backend.application.AskSchoolAssistant;
import com.recred.backend.application.SchoolAssistantCommand;
import com.recred.backend.application.SchoolAssistantConversations;
import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.ChatSession;
import com.recred.backend.domain.models.EstadoChatSesion;
import com.recred.backend.domain.models.RolChatMensaje;
import com.recred.backend.domain.models.SchoolAssistantAction;
import com.recred.backend.domain.models.SchoolAssistantActionType;
import com.recred.backend.domain.models.SchoolAssistantAnswer;
import com.recred.backend.domain.models.SchoolAssistantCapability;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.models.SchoolAssistantSuggestion;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.recred.backend.infrastructure.web.SchoolAssistantController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SchoolAssistantControllerTest {

    private AskSchoolAssistant askSchoolAssistant;
    private SchoolAssistantConversations conversations;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        askSchoolAssistant = mock(AskSchoolAssistant.class);
        conversations = mock(SchoolAssistantConversations.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SchoolAssistantController(askSchoolAssistant, conversations))
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
                "gemini-test",
                SchoolAssistantRole.ALUMNO,
                SchoolAssistantIntent.BALANCE,
                SchoolAssistantAction.informative(
                        SchoolAssistantActionType.CONSULTA_SALDO,
                        Map.of("saldo", "500.00")
                ),
                List.of(new SchoolAssistantSuggestion(
                        "Ultimas compras",
                        "mostrame mis ultimas compras",
                        SchoolAssistantActionType.CONSULTA_COMPRAS
                )),
                Map.of("saldo", "500.00")
        );
        when(askSchoolAssistant.execute(eq(studentId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(answer);

        mockMvc.perform(post("/api/v1/ia/alumnos/{alumnoId}/asistente/mensajes", studentId)
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
                .andExpect(jsonPath("$.modelo").value("gemini-test"))
                .andExpect(jsonPath("$.rol").value("ALUMNO"))
                .andExpect(jsonPath("$.intencion").value("BALANCE"))
                .andExpect(jsonPath("$.accion.tipo").value("CONSULTA_SALDO"))
                .andExpect(jsonPath("$.accion.estado").value("INFORMATIVA"))
                .andExpect(jsonPath("$.accion.datos.saldo").value("500.00"))
                .andExpect(jsonPath("$.sugerencias[0].mensaje")
                        .value("mostrame mis ultimas compras"))
                .andExpect(jsonPath("$.datos.saldo").value("500.00"));

        ArgumentCaptor<SchoolAssistantCommand> commandCaptor = ArgumentCaptor
                .forClass(SchoolAssistantCommand.class);
        verify(askSchoolAssistant).execute(eq(studentId), commandCaptor.capture());
        SchoolAssistantCommand command = commandCaptor.getValue();
        assertEquals("saldo", command.message());
        assertEquals(Optional.of(sessionId), command.sessionId());
        assertTrue(command.sessionId().isPresent());
    }

    @Test
    void askTutorDelegatesToTutorUseCase() throws Exception {
        UUID sessionId = UUID.randomUUID();
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("tutor-sub", "");
        SchoolAssistantAnswer answer = new SchoolAssistantAnswer(
                sessionId,
                "Tu hijo tiene $500.00.",
                List.of(SchoolAssistantCapability.HIJOS),
                LocalDateTime.of(2026, 5, 27, 12, 0),
                true,
                "gemini-test"
        );
        when(askSchoolAssistant.executeForTutor(
                eq("tutor-sub"),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(answer);

        mockMvc.perform(post("/api/v1/ia/tutores/me/asistente/mensajes")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mensaje": "saldo de mis hijos"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sesionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.capacidades[0]").value("HIJOS"));

        verify(askSchoolAssistant).executeForTutor(
                eq("tutor-sub"),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void askKiosqueroDelegatesToKiosqueroUseCase() throws Exception {
        UUID sessionId = UUID.randomUUID();
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("kiosquero-sub", "");
        SchoolAssistantAnswer answer = new SchoolAssistantAnswer(
                sessionId,
                "Tenes 7 productos en stock.",
                List.of(SchoolAssistantCapability.STOCK),
                LocalDateTime.of(2026, 5, 27, 12, 0),
                true,
                "gemini-test"
        );
        when(askSchoolAssistant.executeForKiosquero(
                eq("kiosquero-sub"),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(answer);

        mockMvc.perform(post("/api/v1/ia/kiosqueros/me/asistente/mensajes")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mensaje": "stock"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sesionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.capacidades[0]").value("STOCK"));

        verify(askSchoolAssistant).executeForKiosquero(
                eq("kiosquero-sub"),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void studentSessionsReturnsSessions() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2026, 5, 27, 12, 0);
        ChatSession session = new ChatSession(
                sessionId,
                EstadoChatSesion.ABIERTA,
                now.minusMinutes(5),
                now
        );
        when(conversations.findStudentSessions(studentId, 0, 20))
                .thenReturn(List.of(session));

        mockMvc.perform(get("/api/v1/ia/alumnos/{alumnoId}/asistente/sesiones", studentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sesionId").value(sessionId.toString()))
                .andExpect(jsonPath("$[0].estado").value("ABIERTA"));
    }

    @Test
    void studentMessagesReturnsMessages() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        ChatMessage message = new ChatMessage(
                messageId,
                sessionId,
                RolChatMensaje.USUARIO,
                "saldo",
                null,
                LocalDateTime.of(2026, 5, 27, 12, 0)
        );
        when(conversations.findStudentMessages(studentId, sessionId, 0, 20))
                .thenReturn(List.of(message));

        mockMvc.perform(get(
                        "/api/v1/ia/alumnos/{alumnoId}/asistente/sesiones/{sesionId}/mensajes",
                        studentId,
                        sessionId
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(messageId.toString()))
                .andExpect(jsonPath("$[0].contenido").value("saldo"));
    }

    @Test
    void closeAndDeleteStudentSessionReturnNoContent() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        mockMvc.perform(patch(
                        "/api/v1/ia/alumnos/{alumnoId}/asistente/sesiones/{sesionId}/cerrar",
                        studentId,
                        sessionId
                ))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete(
                        "/api/v1/ia/alumnos/{alumnoId}/asistente/sesiones/{sesionId}",
                        studentId,
                        sessionId
                ))
                .andExpect(status().isNoContent());

        verify(conversations).closeStudentSession(studentId, sessionId);
        verify(conversations).deleteStudentSession(studentId, sessionId);
    }
}
