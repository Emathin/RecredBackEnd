package com.recred.backend.web;

import com.recred.backend.application.AskSchoolAssistant;
import com.recred.backend.application.SchoolAssistantCommand;
import com.recred.backend.application.SchoolAssistantConversations;
import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.ChatSession;
import com.recred.backend.domain.models.EstadoChatSesion;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.RolChatMensaje;
import com.recred.backend.domain.models.SchoolAssistantAction;
import com.recred.backend.domain.models.SchoolAssistantActionStatus;
import com.recred.backend.domain.models.SchoolAssistantActionType;
import com.recred.backend.domain.models.SchoolAssistantAnswer;
import com.recred.backend.domain.models.SchoolAssistantCapability;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.models.SchoolAssistantSuggestion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.recred.backend.infrastructure.web.SchoolAssistantController;
import com.recred.backend.infrastructure.web.SchoolAssistantMessageResponse;
import com.recred.backend.infrastructure.web.SchoolAssistantSessionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private SchoolAssistantController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        askSchoolAssistant = mock(AskSchoolAssistant.class);
        conversations = mock(SchoolAssistantConversations.class);
        controller = new SchoolAssistantController(askSchoolAssistant, conversations);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void askDelegatesMessageToUseCase() throws Exception {
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("student-sub", "");
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
        when(askSchoolAssistant.executeForStudent(
                eq("student-sub"),
                org.mockito.ArgumentMatchers.any()
        ))
                .thenReturn(answer);

        mockMvc.perform(post("/api/v1/ia/alumnos/me/asistente/mensajes")
                        .principal(authentication)
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
        verify(askSchoolAssistant).executeForStudent(eq("student-sub"), commandCaptor.capture());
        SchoolAssistantCommand command = commandCaptor.getValue();
        assertEquals("saldo", command.message());
        assertEquals(Optional.of(sessionId), command.sessionId());
        assertTrue(command.sessionId().isPresent());
    }

    @Test
    void askReturnsReadableRecessLabelForAdvancePurchaseAction() throws Exception {
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("student-sub", "");
        UUID sessionId = UUID.randomUUID();
        UUID purchaseId = UUID.randomUUID();
        SchoolAssistantAction action = new SchoolAssistantAction(
                SchoolAssistantActionType.COMPRA_ANTICIPADA,
                SchoolAssistantActionStatus.EJECUTADA,
                false,
                List.of(),
                purchaseId,
                EstadoCompra.PENDIENTE,
                "TORO",
                new BigDecimal("5250.00"),
                RecessTime.SECOND_RECESS,
                Map.of()
        );
        SchoolAssistantAnswer answer = new SchoolAssistantAnswer(
                sessionId,
                "Compra creada.",
                List.of(SchoolAssistantCapability.COMPRAS),
                LocalDateTime.of(2026, 5, 27, 12, 0),
                false,
                "fixed",
                SchoolAssistantRole.ALUMNO,
                SchoolAssistantIntent.CONFIRM_PURCHASE,
                action,
                List.of(),
                Map.of()
        );
        when(askSchoolAssistant.executeForStudent(
                eq("student-sub"),
                org.mockito.ArgumentMatchers.any()
        ))
                .thenReturn(answer);

        mockMvc.perform(post("/api/v1/ia/alumnos/me/asistente/mensajes")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mensaje": "confirmar"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accion.recreo").value("segundo recreo"));
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
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("student-sub", "");
        UUID sessionId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2026, 5, 27, 12, 0);
        ChatSession session = new ChatSession(
                sessionId,
                EstadoChatSesion.ABIERTA,
                now.minusMinutes(5),
                now
        );
        when(conversations.findStudentSessions("student-sub", 0, 20))
                .thenReturn(List.of(session));

        mockMvc.perform(get("/api/v1/ia/alumnos/me/asistente/sesiones")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sesionId").value(sessionId.toString()))
                .andExpect(jsonPath("$[0].estado").value("ABIERTA"));
    }

    @Test
    void studentMessagesReturnsMessages() throws Exception {
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("student-sub", "");
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
        when(conversations.findStudentMessages("student-sub", sessionId, 0, 20))
                .thenReturn(List.of(message));

        mockMvc.perform(get(
                        "/api/v1/ia/alumnos/me/asistente/sesiones/{sesionId}/mensajes",
                        sessionId
                ).principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(messageId.toString()))
                .andExpect(jsonPath("$[0].contenido").value("saldo"));
    }

    @Test
    void closeAndDeleteStudentSessionReturnNoContent() throws Exception {
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("student-sub", "");
        UUID sessionId = UUID.randomUUID();

        mockMvc.perform(patch(
                        "/api/v1/ia/alumnos/me/asistente/sesiones/{sesionId}/cerrar",
                        sessionId
                ).principal(authentication))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete(
                        "/api/v1/ia/alumnos/me/asistente/sesiones/{sesionId}",
                        sessionId
                ).principal(authentication))
                .andExpect(status().isNoContent());

        verify(conversations).closeStudentSession("student-sub", sessionId);
        verify(conversations).deleteStudentSession("student-sub", sessionId);
    }

    @Test
    void tutorSessionEndpointsDelegateToAuthenticatedSubject() {
        UUID sessionId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("tutor-sub", "");
        ChatSession session = new ChatSession(
                sessionId,
                EstadoChatSesion.CERRADA,
                LocalDateTime.of(2026, 5, 27, 11, 0),
                LocalDateTime.of(2026, 5, 27, 12, 0)
        );
        ChatMessage message = new ChatMessage(
                messageId,
                sessionId,
                RolChatMensaje.ASISTENTE_IA,
                "respuesta",
                Map.of("tipo", "saldo"),
                LocalDateTime.of(2026, 5, 27, 12, 0)
        );
        when(conversations.findTutorSessions("tutor-sub", 1, 5))
                .thenReturn(List.of(session));
        when(conversations.findTutorMessages("tutor-sub", sessionId, 2, 10))
                .thenReturn(List.of(message));

        List<SchoolAssistantSessionResponse> sessions =
                controller.tutorSessions(authentication, 1, 5);
        List<SchoolAssistantMessageResponse> messages =
                controller.tutorMessages(authentication, sessionId, 2, 10);

        assertEquals(sessionId, sessions.get(0).sessionId());
        assertEquals(EstadoChatSesion.CERRADA, sessions.get(0).status());
        assertEquals(messageId, messages.get(0).id());
        assertEquals(RolChatMensaje.ASISTENTE_IA, messages.get(0).role());
        assertEquals(204, controller.closeTutorSession(authentication, sessionId)
                .getStatusCode().value());
        assertEquals(204, controller.deleteTutorSession(authentication, sessionId)
                .getStatusCode().value());
        verify(conversations).closeTutorSession("tutor-sub", sessionId);
        verify(conversations).deleteTutorSession("tutor-sub", sessionId);
    }

    @Test
    void kiosqueroSessionEndpointsDelegateToAuthenticatedSubject() {
        UUID sessionId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("kiosquero-sub", "");
        ChatSession session = new ChatSession(
                sessionId,
                EstadoChatSesion.ABIERTA,
                LocalDateTime.of(2026, 5, 27, 11, 0),
                LocalDateTime.of(2026, 5, 27, 12, 0)
        );
        ChatMessage message = new ChatMessage(
                messageId,
                sessionId,
                RolChatMensaje.USUARIO,
                "stock",
                null,
                LocalDateTime.of(2026, 5, 27, 12, 0)
        );
        when(conversations.findKiosqueroSessions("kiosquero-sub", 1, 5))
                .thenReturn(List.of(session));
        when(conversations.findKiosqueroMessages("kiosquero-sub", sessionId, 2, 10))
                .thenReturn(List.of(message));

        List<SchoolAssistantSessionResponse> sessions =
                controller.kiosqueroSessions(authentication, 1, 5);
        List<SchoolAssistantMessageResponse> messages =
                controller.kiosqueroMessages(authentication, sessionId, 2, 10);

        assertEquals(sessionId, sessions.get(0).sessionId());
        assertEquals(EstadoChatSesion.ABIERTA, sessions.get(0).status());
        assertEquals(messageId, messages.get(0).id());
        assertEquals(RolChatMensaje.USUARIO, messages.get(0).role());
        assertEquals(204, controller.closeKiosqueroSession(authentication, sessionId)
                .getStatusCode().value());
        assertEquals(204, controller.deleteKiosqueroSession(authentication, sessionId)
                .getStatusCode().value());
        verify(conversations).closeKiosqueroSession("kiosquero-sub", sessionId);
        verify(conversations).deleteKiosqueroSession("kiosquero-sub", sessionId);
    }

    @Test
    void tutorEndpointWithoutAuthenticationThrowsCredentialsException() {
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> controller.tutorSessions(null, 0, 20));
    }
}
