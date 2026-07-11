package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.AskSchoolAssistant;
import com.recred.backend.application.AskSchoolAssistantRequest;
import com.recred.backend.application.AskSchoolAssistantResponse;
import com.recred.backend.application.SchoolAssistantConversationMessageResponse;
import com.recred.backend.application.SchoolAssistantConversationSessionResponse;
import com.recred.backend.application.SchoolAssistantConversations;
import com.recred.backend.domain.models.EstadoChatSesion;
import com.recred.backend.domain.models.RolChatMensaje;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.infrastructure.web.PlanAccessGuard;
import com.recred.backend.infrastructure.web.SchoolAssistantController;
import com.recred.backend.infrastructure.web.SchoolAssistantMessageResponse;
import com.recred.backend.infrastructure.web.SchoolAssistantRequest;
import com.recred.backend.infrastructure.web.SchoolAssistantResponse;
import com.recred.backend.infrastructure.web.SchoolAssistantSessionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolAssistantControllerTest {

    private static final String STUDENT_SUBJECT = "student-sub";
    private static final String TUTOR_SUBJECT = "tutor-sub";
    private static final String KIOSQUERO_SUBJECT = "kiosquero-sub";
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID MESSAGE_ID = UUID.randomUUID();

    @Mock private AskSchoolAssistant askSchoolAssistant;
    @Mock private SchoolAssistantConversations conversations;
    @Mock private PlanAccessGuard planAccessGuard;

    private SchoolAssistantController controller;

    @BeforeEach
    void setUp() {
        controller = new SchoolAssistantController(
                askSchoolAssistant,
                conversations,
                planAccessGuard
        );
    }

    @Test
    void askDelegatesMessageToUseCaseAndReturnsResponse() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(STUDENT_SUBJECT, "");
        SchoolAssistantRequest request = givenAssistantRequest("saldo", SESSION_ID);
        givenAskSchoolAssistantReturns(SESSION_ID, "Tenes $500.00 disponibles.", List.of("SALDO"),
                "ALUMNO", "BALANCE", givenBalanceActionResponse());

        SchoolAssistantResponse response =
                controller.askStudent(authentication, request);

        thenAskResponseIsOkAndMatches(response, SESSION_ID, "Tenes $500.00 disponibles.", "BALANCE");
        thenDelegatesToAskSchoolAssistant(STUDENT_SUBJECT, SchoolAssistantRole.ALUMNO, "saldo");
    }

    @Test
    void askReturnsReadableRecessLabelForAdvancePurchaseAction() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(STUDENT_SUBJECT, "");
        SchoolAssistantRequest request = givenAssistantRequest("confirmar", null);
        givenAskSchoolAssistantReturns(SESSION_ID, "Compra creada.", List.of("COMPRAS"),
                "ALUMNO", "CONFIRM_PURCHASE", givenAdvancePurchaseActionResponse());

        SchoolAssistantResponse response =
                controller.askStudent(authentication, request);

        assertEquals("segundo recreo", response.action().recessTime());
    }

    @Test
    void askTutorDelegatesToTutorUseCase() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(TUTOR_SUBJECT, "");
        SchoolAssistantRequest request = givenAssistantRequest("saldo de mis hijos", null);
        givenAskSchoolAssistantReturns(SESSION_ID, "Tu hijo tiene $500.00.", List.of("HIJOS"),
                "TUTOR", "UNKNOWN", null);

        SchoolAssistantResponse response =
                controller.askTutor(authentication, request);

        assertEquals(SESSION_ID, response.sessionId());
        thenDelegatesToAskSchoolAssistant(TUTOR_SUBJECT, SchoolAssistantRole.TUTOR, "saldo de mis hijos");
    }

    @Test
    void askKiosqueroDelegatesToKiosqueroUseCaseAndChecksGuard() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(KIOSQUERO_SUBJECT, "");
        SchoolAssistantRequest request = givenAssistantRequest("stock", null);
        givenAskSchoolAssistantReturns(SESSION_ID, "Tenes 7 productos en stock.", List.of("STOCK"),
                "KIOSQUERO", "UNKNOWN", null);

        SchoolAssistantResponse response =
                controller.askKiosquero(authentication, request);

        thenDelegatesToAskSchoolAssistant(KIOSQUERO_SUBJECT, SchoolAssistantRole.KIOSQUERO, "stock");
        verify(planAccessGuard).requireVendorIntermediate(authentication);
    }

    @Test
    void studentSessionsReturnsSessions() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(STUDENT_SUBJECT, "");
        givenStudentSessions();

        List<SchoolAssistantSessionResponse> sessions = controller.studentSessions(authentication, 0, 20);

        assertEquals(1, sessions.size());
        assertEquals(SESSION_ID, sessions.get(0).sessionId());
        assertEquals(EstadoChatSesion.ABIERTA, sessions.get(0).status());
    }

    @Test
    void studentMessagesReturnsMessages() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(STUDENT_SUBJECT, "");
        givenStudentMessages();

        List<SchoolAssistantMessageResponse> messages =
                controller.studentMessages(authentication, SESSION_ID, 0, 20);

        assertEquals(1, messages.size());
        assertEquals(MESSAGE_ID, messages.get(0).id());
        assertEquals("saldo", messages.get(0).content());
    }

    @Test
    void closeAndDeleteStudentSessionReturnNoContent() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(STUDENT_SUBJECT, "");

        ResponseEntity<Void> closeResponse = controller.closeStudentSession(authentication, SESSION_ID);
        ResponseEntity<Void> deleteResponse = controller.deleteStudentSession(authentication, SESSION_ID);

        assertEquals(HttpStatus.NO_CONTENT, closeResponse.getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());
        verify(conversations).closeStudentSession(STUDENT_SUBJECT, SESSION_ID);
        verify(conversations).deleteStudentSession(STUDENT_SUBJECT, SESSION_ID);
    }

    @Test
    void tutorSessionEndpointsDelegateToAuthenticatedSubject() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(TUTOR_SUBJECT, "");
        givenTutorSessions();
        givenTutorMessages();

        List<SchoolAssistantSessionResponse> sessions = controller.tutorSessions(authentication, 1, 5);
        List<SchoolAssistantMessageResponse> messages =
                controller.tutorMessages(authentication, SESSION_ID, 2, 10);
        ResponseEntity<Void> closeResponse = controller.closeTutorSession(authentication, SESSION_ID);
        ResponseEntity<Void> deleteResponse = controller.deleteTutorSession(authentication, SESSION_ID);

        assertEquals(SESSION_ID, sessions.get(0).sessionId());
        assertEquals(MESSAGE_ID, messages.get(0).id());
        assertEquals(HttpStatus.NO_CONTENT, closeResponse.getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());
        verify(conversations).closeTutorSession(TUTOR_SUBJECT, SESSION_ID);
        verify(conversations).deleteTutorSession(TUTOR_SUBJECT, SESSION_ID);
    }

    @Test
    void kiosqueroSessionEndpointsDelegateToAuthenticatedSubject() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(KIOSQUERO_SUBJECT, "");
        givenKiosqueroSessions();
        givenKiosqueroMessages();

        List<SchoolAssistantSessionResponse> sessions = controller.kiosqueroSessions(authentication, 1, 5);
        List<SchoolAssistantMessageResponse> messages =
                controller.kiosqueroMessages(authentication, SESSION_ID, 2, 10);
        ResponseEntity<Void> closeResponse = controller.closeKiosqueroSession(authentication, SESSION_ID);
        ResponseEntity<Void> deleteResponse = controller.deleteKiosqueroSession(authentication, SESSION_ID);

        assertEquals(SESSION_ID, sessions.get(0).sessionId());
        assertEquals(MESSAGE_ID, messages.get(0).id());
        assertEquals(HttpStatus.NO_CONTENT, closeResponse.getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());
        verify(conversations).closeKiosqueroSession(KIOSQUERO_SUBJECT, SESSION_ID);
        verify(conversations).deleteKiosqueroSession(KIOSQUERO_SUBJECT, SESSION_ID);
    }

    @Test
    void tutorEndpointWithoutAuthenticationThrowsCredentialsException() {
        Executable execution = () -> controller.tutorSessions(null, 0, 20);

        assertThrows(AuthenticationCredentialsNotFoundException.class, execution);
    }

    private SchoolAssistantRequest givenAssistantRequest(String message, UUID sessionId) {
        return new SchoolAssistantRequest(sessionId, message);
    }

    private void givenAskSchoolAssistantReturns(
            UUID sessionId,
            String textResponse,
            List<String> capabilities,
            String role,
            String intent,
            AskSchoolAssistantResponse.ActionResponse action
    ) {
        LocalDateTime dateTime = LocalDateTime.of(2026, 5, 27, 12, 0);
        AskSchoolAssistantResponse answer = new AskSchoolAssistantResponse(
                sessionId,
                textResponse,
                capabilities,
                dateTime,
                true,
                "gemini-test",
                role,
                intent,
                action,
                List.of(new AskSchoolAssistantResponse.SuggestionResponse(
                        "Ultimas compras",
                        "mostrame mis ultimas compras",
                        "CONSULTA_COMPRAS"
                )),
                Map.of("saldo", "500.00")
        );
        when(askSchoolAssistant.execute(any())).thenReturn(answer);
    }

    private AskSchoolAssistantResponse.ActionResponse givenBalanceActionResponse() {
        return new AskSchoolAssistantResponse.ActionResponse(
                "CONSULTA_SALDO",
                "INFORMATIVA",
                false,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                Map.of("saldo", "500.00")
        );
    }

    private AskSchoolAssistantResponse.ActionResponse givenAdvancePurchaseActionResponse() {
        return new AskSchoolAssistantResponse.ActionResponse(
                "COMPRA_ANTICIPADA",
                "EJECUTADA",
                false,
                List.of(),
                UUID.randomUUID(),
                "PENDIENTE",
                "TORO",
                new BigDecimal("5250.00"),
                "segundo recreo",
                Map.of()
        );
    }

    private void givenStudentSessions() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 27, 12, 0);
        SchoolAssistantConversationSessionResponse session = new SchoolAssistantConversationSessionResponse(
                SESSION_ID, EstadoChatSesion.ABIERTA, now.minusMinutes(5), now
        );
        when(conversations.findStudentSessions(STUDENT_SUBJECT, 0, 20)).thenReturn(List.of(session));
    }

    private void givenStudentMessages() {
        SchoolAssistantConversationMessageResponse message = new SchoolAssistantConversationMessageResponse(
                MESSAGE_ID, SESSION_ID, RolChatMensaje.USUARIO, "saldo", null, LocalDateTime.now()
        );
        when(conversations.findStudentMessages(STUDENT_SUBJECT, SESSION_ID, 0, 20)).thenReturn(List.of(message));
    }

    private void givenTutorSessions() {
        SchoolAssistantConversationSessionResponse session = new SchoolAssistantConversationSessionResponse(
                SESSION_ID, EstadoChatSesion.CERRADA, LocalDateTime.now(), LocalDateTime.now()
        );
        when(conversations.findTutorSessions(TUTOR_SUBJECT, 1, 5)).thenReturn(List.of(session));
    }

    private void givenTutorMessages() {
        SchoolAssistantConversationMessageResponse message = new SchoolAssistantConversationMessageResponse(
                MESSAGE_ID, SESSION_ID, RolChatMensaje.ASISTENTE_IA, "respuesta", Map.of(), LocalDateTime.now()
        );
        when(conversations.findTutorMessages(TUTOR_SUBJECT, SESSION_ID, 2, 10)).thenReturn(List.of(message));
    }

    private void givenKiosqueroSessions() {
        SchoolAssistantConversationSessionResponse session = new SchoolAssistantConversationSessionResponse(
                SESSION_ID, EstadoChatSesion.ABIERTA, LocalDateTime.now(), LocalDateTime.now()
        );
        when(conversations.findKiosqueroSessions(KIOSQUERO_SUBJECT, 1, 5)).thenReturn(List.of(session));
    }

    private void givenKiosqueroMessages() {
        SchoolAssistantConversationMessageResponse message = new SchoolAssistantConversationMessageResponse(
                MESSAGE_ID, SESSION_ID, RolChatMensaje.USUARIO, "stock", null, LocalDateTime.now()
        );
        when(conversations.findKiosqueroMessages(KIOSQUERO_SUBJECT, SESSION_ID, 2, 10)).thenReturn(List.of(message));
    }

    private void thenAskResponseIsOkAndMatches(
            SchoolAssistantResponse response,
            UUID expectedSessionId,
            String expectedText,
            String expectedIntent
    ) {
        assertEquals(expectedSessionId, response.sessionId());
        assertEquals(expectedText, response.answer());
        assertEquals(expectedIntent, response.intent());
    }

    private void thenDelegatesToAskSchoolAssistant(
            String expectedSubject,
            SchoolAssistantRole expectedRole,
            String expectedMessage
    ) {
        ArgumentCaptor<AskSchoolAssistantRequest> requestCaptor =
                ArgumentCaptor.forClass(AskSchoolAssistantRequest.class);
        verify(askSchoolAssistant).execute(requestCaptor.capture());
        AskSchoolAssistantRequest useCaseRequest = requestCaptor.getValue();
        assertEquals(expectedSubject, useCaseRequest.cognitoSubject());
        assertEquals(expectedRole, useCaseRequest.role());
        assertEquals(expectedMessage, useCaseRequest.message());
    }
}
