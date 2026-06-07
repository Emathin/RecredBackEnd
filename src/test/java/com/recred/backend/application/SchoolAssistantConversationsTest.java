package com.recred.backend.application;

import com.recred.backend.domain.exceptions.ChatSessionNotFoundException;
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
}
