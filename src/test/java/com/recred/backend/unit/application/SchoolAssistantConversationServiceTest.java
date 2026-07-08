package com.recred.backend.unit.application;

import com.recred.backend.application.SchoolAssistantConversation;
import com.recred.backend.application.SchoolAssistantConversationService;
import com.recred.backend.domain.exceptions.ChatSessionNotFoundException;
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
import com.recred.backend.domain.repositories.ChatMessageRepository;
import com.recred.backend.domain.repositories.ChatSessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolAssistantConversationServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 27, 12, 0);

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    private SchoolAssistantConversationService conversationService;
    private UUID studentId;
    private UUID userId;
    private UUID sessionId;
    private ChatSession session;
    private List<ChatMessage> recentMessages;
    private SchoolAssistantConversation conversation;
    private Executable execution;
    private SchoolAssistantAnswer answer;
    private List<SchoolAssistantCapability> capabilities;
    private Map<String, Object> additionalMetadata;
    private List<ChatMessage> storedMessages;

    @BeforeEach
    void setUp() {
        conversationService = new SchoolAssistantConversationService(
                chatSessionRepository,
                chatMessageRepository
        );
        studentId = UUID.randomUUID();
        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        session = new ChatSession(sessionId, EstadoChatSesion.ABIERTA, NOW, NOW);
        capabilities = List.of(
                SchoolAssistantCapability.SALDO,
                SchoolAssistantCapability.COMPRAS
        );
        additionalMetadata = Map.of("intencionChatbot", "BALANCE");
    }

    @Test
    void startForStudent_WhenSessionIsNew_ShouldStartSessionAndLoadRecentMessages() {
        givenNewStudentSession();
        givenRecentMessages();

        whenStartingNewStudentConversation();

        thenStudentSessionWasStarted();
        thenConversationHasSession();
        thenConversationHasRecentMessages();
    }

    @Test
    void startForStudent_WhenSessionExists_ShouldUseOpenStudentSession() {
        givenOpenStudentSession();
        givenRecentMessages();

        whenStartingExistingStudentConversation();

        thenOpenStudentSessionWasUsed();
        thenConversationHasSession();
        thenConversationHasRecentMessages();
    }

    @Test
    void startForStudent_WhenSessionDoesNotExist_ShouldThrowException() {
        givenOpenStudentSessionDoesNotExist();

        whenStartingExistingStudentConversationFails();

        thenChatSessionNotFoundExceptionIsThrown();
    }

    @Test
    void startForUser_WhenSessionIsNew_ShouldStartSessionAndLoadRecentMessages() {
        givenNewUserSession();
        givenRecentMessages();

        whenStartingNewUserConversation();

        thenUserSessionWasStarted();
        thenConversationHasSession();
        thenConversationHasRecentMessages();
    }

    @Test
    void startForUser_WhenSessionExists_ShouldUseOpenUserSession() {
        givenOpenUserSession();
        givenRecentMessages();

        whenStartingExistingUserConversation();

        thenOpenUserSessionWasUsed();
        thenConversationHasSession();
        thenConversationHasRecentMessages();
    }

    @Test
    void saveConversation_WhenAnswerHasMetadata_ShouldStoreMessagesAndUpdateActivity() {
        givenConversation();
        givenAssistantAnswer();

        whenSavingConversation();

        thenUserMessageWasStored();
        thenAssistantMessageWasStored();
        thenSessionActivityWasUpdated();
    }

    private void givenNewStudentSession() {
        when(chatSessionRepository.startForStudent(studentId, NOW)).thenReturn(session);
    }

    private void givenOpenStudentSession() {
        when(chatSessionRepository.findOpenByIdAndStudentId(sessionId, studentId))
                .thenReturn(Optional.of(session));
    }

    private void givenOpenStudentSessionDoesNotExist() {
        when(chatSessionRepository.findOpenByIdAndStudentId(sessionId, studentId))
                .thenReturn(Optional.empty());
    }

    private void givenNewUserSession() {
        when(chatSessionRepository.startForUser(userId, NOW)).thenReturn(session);
    }

    private void givenOpenUserSession() {
        when(chatSessionRepository.findOpenByIdAndUserId(sessionId, userId))
                .thenReturn(Optional.of(session));
    }

    private void givenRecentMessages() {
        recentMessages = List.of(userMessage("saldo"));
        when(chatMessageRepository.findRecentBySessionId(sessionId, 6))
                .thenReturn(recentMessages);
    }

    private void givenConversation() {
        conversation = new SchoolAssistantConversation(session, List.of());
    }

    private void givenAssistantAnswer() {
        answer = new SchoolAssistantAnswer(
                sessionId,
                "Tenes saldo.",
                capabilities,
                NOW,
                true,
                "gemini-test",
                SchoolAssistantRole.ALUMNO,
                SchoolAssistantIntent.BALANCE,
                SchoolAssistantAction.informative(
                        SchoolAssistantActionType.CONSULTA_SALDO,
                        Map.of("saldo", "500")
                ),
                List.of(),
                Map.of("saldo", "500")
        );
    }

    private void whenStartingNewStudentConversation() {
        conversation = conversationService.startForStudent(studentId, null, NOW);
    }

    private void whenStartingExistingStudentConversation() {
        conversation = conversationService.startForStudent(studentId, sessionId, NOW);
    }

    private void whenStartingExistingStudentConversationFails() {
        execution = () -> conversationService.startForStudent(studentId, sessionId, NOW);
    }

    private void whenStartingNewUserConversation() {
        conversation = conversationService.startForUser(userId, null, NOW);
    }

    private void whenStartingExistingUserConversation() {
        conversation = conversationService.startForUser(userId, sessionId, NOW);
    }

    private void whenSavingConversation() {
        conversationService.saveConversation(
                conversation,
                "saldo",
                capabilities,
                answer,
                additionalMetadata,
                NOW
        );
    }

    private void thenStudentSessionWasStarted() {
        verify(chatSessionRepository).startForStudent(studentId, NOW);
    }

    private void thenOpenStudentSessionWasUsed() {
        verify(chatSessionRepository).findOpenByIdAndStudentId(sessionId, studentId);
    }

    private void thenChatSessionNotFoundExceptionIsThrown() {
        assertThrows(ChatSessionNotFoundException.class, execution);
    }

    private void thenUserSessionWasStarted() {
        verify(chatSessionRepository).startForUser(userId, NOW);
    }

    private void thenOpenUserSessionWasUsed() {
        verify(chatSessionRepository).findOpenByIdAndUserId(sessionId, userId);
    }

    private void thenConversationHasSession() {
        assertEquals(session, conversation.session());
        assertEquals(sessionId, conversation.sessionId());
    }

    private void thenConversationHasRecentMessages() {
        assertEquals(recentMessages, conversation.recentMessages());
    }

    private void thenUserMessageWasStored() {
        ChatMessage message = storedMessages().get(0);
        assertEquals(sessionId, message.sessionId());
        assertEquals(RolChatMensaje.USUARIO, message.role());
        assertEquals("saldo", message.content());
        assertEquals(List.of("SALDO", "COMPRAS"),
                message.metadata().get("capacidadesPermitidas"));
    }

    private void thenAssistantMessageWasStored() {
        ChatMessage message = storedMessages().get(1);
        Map<?, ?> metadata = message.metadata();
        assertEquals(sessionId, message.sessionId());
        assertEquals(RolChatMensaje.ASISTENTE_IA, message.role());
        assertEquals("Tenes saldo.", message.content());
        assertEquals(true, metadata.get("generadoPorIa"));
        assertEquals("gemini-test", metadata.get("modelo"));
        assertEquals("ALUMNO", metadata.get("rol"));
        assertEquals("BALANCE", metadata.get("intencion"));
        assertEquals("BALANCE", metadata.get("intencionChatbot"));
        thenAssistantActionMetadataWasStored(metadata);
    }

    private void thenAssistantActionMetadataWasStored(Map<?, ?> metadata) {
        Map<?, ?> action = (Map<?, ?>) metadata.get("accion");
        assertEquals("CONSULTA_SALDO", action.get("tipo"));
        assertEquals("INFORMATIVA", action.get("estado"));
    }

    private void thenSessionActivityWasUpdated() {
        verify(chatSessionRepository).updateLastActivity(sessionId, NOW);
    }

    private List<ChatMessage> storedMessages() {
        if (storedMessages == null) {
            ArgumentCaptor<ChatMessage> messageCaptor =
                    ArgumentCaptor.forClass(ChatMessage.class);
            verify(chatMessageRepository, times(2)).save(messageCaptor.capture());
            storedMessages = messageCaptor.getAllValues();
        }

        return storedMessages;
    }

    private ChatMessage userMessage(String content) {
        return new ChatMessage(
                sessionId,
                RolChatMensaje.USUARIO,
                content,
                Map.of(),
                NOW
        );
    }
}
