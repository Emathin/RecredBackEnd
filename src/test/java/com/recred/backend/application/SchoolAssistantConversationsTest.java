package com.recred.backend.application;

import com.recred.backend.domain.exceptions.ChatSessionNotFoundException;
import com.recred.backend.domain.exceptions.KiosqueroNotFoundException;
import com.recred.backend.domain.exceptions.TutorNoEncontradoException;
import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.ChatSession;
import com.recred.backend.domain.models.EstadoChatSesion;
import com.recred.backend.domain.models.RolChatMensaje;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ChatMessageRepository;
import com.recred.backend.domain.repositories.ChatSessionRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolAssistantConversationsTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-27T12:00:00Z"),
            ZoneOffset.UTC
    );
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 27, 12, 0);

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private KiosqueroRepository kiosqueroRepository;

    private SchoolAssistantConversations conversations;

    @BeforeEach
    void setUp() {
        conversations = new SchoolAssistantConversations(
                chatSessionRepository,
                chatMessageRepository,
                alumnoRepository,
                usuarioRepository,
                tutorRepository,
                kiosqueroRepository,
                CLOCK
        );
    }

    @Test
    void findStudentMessagesRequiresOwnedSession() {
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        ChatSession session = new ChatSession(
                sessionId,
                EstadoChatSesion.ABIERTA,
                NOW.minusMinutes(5),
                NOW
        );
        ChatMessage message = new ChatMessage(
                messageId,
                sessionId,
                RolChatMensaje.USUARIO,
                "saldo",
                null,
                NOW
        );
        when(alumnoRepository.existsById(studentId)).thenReturn(true);
        when(chatSessionRepository.findByIdAndStudentId(sessionId, studentId))
                .thenReturn(Optional.of(session));
        when(chatMessageRepository.findBySessionId(sessionId, 0, 20))
                .thenReturn(List.of(message));

        List<ChatMessage> messages = conversations.findStudentMessages(
                studentId,
                sessionId,
                0,
                20
        );

        assertEquals(1, messages.size());
        assertEquals(messageId, messages.get(0).id());
    }

    @Test
    void findStudentSessionsResolvesStudentIdFromUserId() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        ChatSession session = new ChatSession(
                sessionId,
                EstadoChatSesion.ABIERTA,
                NOW.minusMinutes(5),
                NOW
        );
        when(alumnoRepository.existsById(userId)).thenReturn(false);
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.of(studentId));
        when(chatSessionRepository.findByStudentId(studentId, 0, 20))
                .thenReturn(List.of(session));

        List<ChatSession> sessions = conversations.findStudentSessions(userId, 0, 20);

        assertEquals(1, sessions.size());
        assertEquals(sessionId, sessions.get(0).id());
        verify(chatSessionRepository).findByStudentId(studentId, 0, 20);
    }

    @Test
    void deleteTutorSessionDeletesMessagesThenSession() {
        String cognitoSubject = "tutor-sub";
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Usuario user = new Usuario();
        user.setId(userId);
        Tutor tutor = new Tutor();
        tutor.setId(UUID.randomUUID());
        ChatSession session = new ChatSession(
                sessionId,
                EstadoChatSesion.ABIERTA,
                NOW.minusMinutes(5),
                NOW
        );
        when(usuarioRepository.findByIdCognito(cognitoSubject)).thenReturn(Optional.of(user));
        when(tutorRepository.findByUsuarioIdCognito(cognitoSubject)).thenReturn(Optional.of(tutor));
        when(chatSessionRepository.findByIdAndUserId(sessionId, userId))
                .thenReturn(Optional.of(session));

        conversations.deleteTutorSession(cognitoSubject, sessionId);

        verify(chatMessageRepository).deleteBySessionId(sessionId);
        verify(chatSessionRepository).deleteById(sessionId);
    }

    @Test
    void closeKiosqueroSessionRequiresOwnedSession() {
        String cognitoSubject = "seller-sub";
        UUID userId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Usuario user = new Usuario();
        user.setId(userId);
        when(usuarioRepository.findByIdCognito(cognitoSubject)).thenReturn(Optional.of(user));
        when(kiosqueroRepository.findBuffetByUsuarioId(userId)).thenReturn(buffetId);
        when(chatSessionRepository.findByIdAndUserId(sessionId, userId))
                .thenReturn(Optional.empty());

        assertThrows(
                ChatSessionNotFoundException.class,
                () -> conversations.closeKiosqueroSession(cognitoSubject, sessionId)
        );
    }

    @Test
    void findTutorSessionsDelegatesByAuthenticatedUserId() {
        String cognitoSubject = "tutor-sub";
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Usuario user = user(userId);
        Tutor tutor = new Tutor();
        tutor.setId(UUID.randomUUID());
        ChatSession session = session(sessionId);
        when(usuarioRepository.findByIdCognito(cognitoSubject)).thenReturn(Optional.of(user));
        when(tutorRepository.findByUsuarioIdCognito(cognitoSubject)).thenReturn(Optional.of(tutor));
        when(chatSessionRepository.findByUserId(userId, 1, 5)).thenReturn(List.of(session));

        List<ChatSession> sessions = conversations.findTutorSessions(cognitoSubject, 1, 5);

        assertEquals(List.of(session), sessions);
        verify(chatSessionRepository).findByUserId(userId, 1, 5);
    }

    @Test
    void findKiosqueroSessionsDelegatesByAuthenticatedUserId() {
        String cognitoSubject = "seller-sub";
        UUID userId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Usuario user = user(userId);
        ChatSession session = session(sessionId);
        when(usuarioRepository.findByIdCognito(cognitoSubject)).thenReturn(Optional.of(user));
        when(kiosqueroRepository.findBuffetByUsuarioId(userId)).thenReturn(buffetId);
        when(chatSessionRepository.findByUserId(userId, 2, 10)).thenReturn(List.of(session));

        List<ChatSession> sessions = conversations.findKiosqueroSessions(cognitoSubject, 2, 10);

        assertEquals(List.of(session), sessions);
        verify(chatSessionRepository).findByUserId(userId, 2, 10);
    }

    @Test
    void findTutorAndKiosqueroMessagesRequireOwnedSessions() {
        String tutorSubject = "tutor-sub";
        String sellerSubject = "seller-sub";
        UUID tutorUserId = UUID.randomUUID();
        UUID sellerUserId = UUID.randomUUID();
        UUID tutorSessionId = UUID.randomUUID();
        UUID sellerSessionId = UUID.randomUUID();
        ChatMessage tutorMessage = message(tutorSessionId, "hijos");
        ChatMessage sellerMessage = message(sellerSessionId, "stock");
        Tutor tutor = new Tutor();
        tutor.setId(UUID.randomUUID());
        when(usuarioRepository.findByIdCognito(tutorSubject))
                .thenReturn(Optional.of(user(tutorUserId)));
        when(tutorRepository.findByUsuarioIdCognito(tutorSubject)).thenReturn(Optional.of(tutor));
        when(chatSessionRepository.findByIdAndUserId(tutorSessionId, tutorUserId))
                .thenReturn(Optional.of(session(tutorSessionId)));
        when(chatMessageRepository.findBySessionId(tutorSessionId, 0, 20))
                .thenReturn(List.of(tutorMessage));
        when(usuarioRepository.findByIdCognito(sellerSubject))
                .thenReturn(Optional.of(user(sellerUserId)));
        when(kiosqueroRepository.findBuffetByUsuarioId(sellerUserId))
                .thenReturn(UUID.randomUUID());
        when(chatSessionRepository.findByIdAndUserId(sellerSessionId, sellerUserId))
                .thenReturn(Optional.of(session(sellerSessionId)));
        when(chatMessageRepository.findBySessionId(sellerSessionId, 1, 5))
                .thenReturn(List.of(sellerMessage));

        List<ChatMessage> tutorMessages = conversations.findTutorMessages(
                tutorSubject,
                tutorSessionId,
                0,
                20
        );
        List<ChatMessage> sellerMessages = conversations.findKiosqueroMessages(
                sellerSubject,
                sellerSessionId,
                1,
                5
        );

        assertEquals(List.of(tutorMessage), tutorMessages);
        assertEquals(List.of(sellerMessage), sellerMessages);
    }

    @Test
    void closeSessionsValidateOwnershipAndCloseWithClockTime() {
        UUID studentId = UUID.randomUUID();
        UUID studentSessionId = UUID.randomUUID();
        String tutorSubject = "tutor-sub";
        String sellerSubject = "seller-sub";
        UUID tutorUserId = UUID.randomUUID();
        UUID sellerUserId = UUID.randomUUID();
        UUID tutorSessionId = UUID.randomUUID();
        UUID sellerSessionId = UUID.randomUUID();
        Tutor tutor = new Tutor();
        tutor.setId(UUID.randomUUID());
        when(alumnoRepository.existsById(studentId)).thenReturn(true);
        when(chatSessionRepository.findByIdAndStudentId(studentSessionId, studentId))
                .thenReturn(Optional.of(session(studentSessionId)));
        when(usuarioRepository.findByIdCognito(tutorSubject))
                .thenReturn(Optional.of(user(tutorUserId)));
        when(tutorRepository.findByUsuarioIdCognito(tutorSubject)).thenReturn(Optional.of(tutor));
        when(chatSessionRepository.findByIdAndUserId(tutorSessionId, tutorUserId))
                .thenReturn(Optional.of(session(tutorSessionId)));
        when(usuarioRepository.findByIdCognito(sellerSubject))
                .thenReturn(Optional.of(user(sellerUserId)));
        when(kiosqueroRepository.findBuffetByUsuarioId(sellerUserId))
                .thenReturn(UUID.randomUUID());
        when(chatSessionRepository.findByIdAndUserId(sellerSessionId, sellerUserId))
                .thenReturn(Optional.of(session(sellerSessionId)));

        conversations.closeStudentSession(studentId, studentSessionId);
        conversations.closeTutorSession(tutorSubject, tutorSessionId);
        conversations.closeKiosqueroSession(sellerSubject, sellerSessionId);

        verify(chatSessionRepository).close(studentSessionId, NOW);
        verify(chatSessionRepository).close(tutorSessionId, NOW);
        verify(chatSessionRepository).close(sellerSessionId, NOW);
    }

    @Test
    void deleteStudentAndKiosqueroSessionsDeleteMessagesThenSession() {
        UUID studentId = UUID.randomUUID();
        UUID studentSessionId = UUID.randomUUID();
        String sellerSubject = "seller-sub";
        UUID sellerUserId = UUID.randomUUID();
        UUID sellerSessionId = UUID.randomUUID();
        when(alumnoRepository.existsById(studentId)).thenReturn(true);
        when(chatSessionRepository.findByIdAndStudentId(studentSessionId, studentId))
                .thenReturn(Optional.of(session(studentSessionId)));
        when(usuarioRepository.findByIdCognito(sellerSubject))
                .thenReturn(Optional.of(user(sellerUserId)));
        when(kiosqueroRepository.findBuffetByUsuarioId(sellerUserId))
                .thenReturn(UUID.randomUUID());
        when(chatSessionRepository.findByIdAndUserId(sellerSessionId, sellerUserId))
                .thenReturn(Optional.of(session(sellerSessionId)));

        conversations.deleteStudentSession(studentId, studentSessionId);
        conversations.deleteKiosqueroSession(sellerSubject, sellerSessionId);

        verify(chatMessageRepository).deleteBySessionId(studentSessionId);
        verify(chatSessionRepository).deleteById(studentSessionId);
        verify(chatMessageRepository).deleteBySessionId(sellerSessionId);
        verify(chatSessionRepository).deleteById(sellerSessionId);
    }

    @Test
    void findStudentSessionsThrowsWhenStudentCannotBeResolvedFromUserId() {
        UUID userId = UUID.randomUUID();
        when(alumnoRepository.existsById(userId)).thenReturn(false);
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(
                com.recred.backend.domain.exceptions.AlumnoNoEncontradoException.class,
                () -> conversations.findStudentSessions(userId, 0, 20)
        );
    }

    @Test
    void tutorOperationsThrowWhenTutorProfileIsMissing() {
        String cognitoSubject = "tutor-sub";
        UUID userId = UUID.randomUUID();
        when(usuarioRepository.findByIdCognito(cognitoSubject))
                .thenReturn(Optional.of(user(userId)));
        when(tutorRepository.findByUsuarioIdCognito(cognitoSubject)).thenReturn(Optional.empty());

        assertThrows(
                TutorNoEncontradoException.class,
                () -> conversations.findTutorSessions(cognitoSubject, 0, 20)
        );
    }

    @Test
    void kiosqueroOperationsThrowWhenBuffetIsMissing() {
        String cognitoSubject = "seller-sub";
        UUID userId = UUID.randomUUID();
        when(usuarioRepository.findByIdCognito(cognitoSubject))
                .thenReturn(Optional.of(user(userId)));
        when(kiosqueroRepository.findBuffetByUsuarioId(userId)).thenReturn(null);

        assertThrows(
                KiosqueroNotFoundException.class,
                () -> conversations.findKiosqueroSessions(cognitoSubject, 0, 20)
        );
    }

    @Test
    void operationsThrowWhenAuthenticatedUserDoesNotExist() {
        String cognitoSubject = "missing-sub";
        when(usuarioRepository.findByIdCognito(cognitoSubject)).thenReturn(Optional.empty());

        assertThrows(
                UsuarioAutenticadoNoEncontradoException.class,
                () -> conversations.findTutorSessions(cognitoSubject, 0, 20)
        );
    }

    private Usuario user(UUID userId) {
        Usuario user = new Usuario();
        user.setId(userId);
        return user;
    }

    private ChatSession session(UUID sessionId) {
        return new ChatSession(
                sessionId,
                EstadoChatSesion.ABIERTA,
                NOW.minusMinutes(5),
                NOW
        );
    }

    private ChatMessage message(UUID sessionId, String content) {
        return new ChatMessage(
                UUID.randomUUID(),
                sessionId,
                RolChatMensaje.USUARIO,
                content,
                null,
                NOW
        );
    }
}
