package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.ChatSessionNotFoundException;
import com.recred.backend.domain.exceptions.KiosqueroNotFoundException;
import com.recred.backend.domain.exceptions.TutorNoEncontradoException;
import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.ChatSession;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ChatMessageRepository;
import com.recred.backend.domain.repositories.ChatSessionRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class SchoolAssistantConversations {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AlumnoRepository alumnoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;
    private final KiosqueroRepository kiosqueroRepository;
    private final Clock clock;

    public SchoolAssistantConversations(
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            AlumnoRepository alumnoRepository,
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository,
            KiosqueroRepository kiosqueroRepository,
            Clock clock
    ) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.alumnoRepository = alumnoRepository;
        this.usuarioRepository = usuarioRepository;
        this.tutorRepository = tutorRepository;
        this.kiosqueroRepository = kiosqueroRepository;
        this.clock = clock;
    }

    public List<ChatSession> findStudentSessions(UUID studentIdOrUserId, int page, int size) {
        UUID studentId = resolvedStudentId(studentIdOrUserId);
        return chatSessionRepository.findByStudentId(studentId, page, size);
    }

    public List<ChatSession> findStudentSessions(String cognitoSubject, int page, int size) {
        UUID studentId = authenticatedStudentId(cognitoSubject);
        return chatSessionRepository.findByStudentId(studentId, page, size);
    }

    public List<ChatSession> findTutorSessions(String cognitoSubject, int page, int size) {
        return chatSessionRepository.findByUserId(tutorUserId(cognitoSubject), page, size);
    }

    public List<ChatSession> findKiosqueroSessions(String cognitoSubject, int page, int size) {
        return chatSessionRepository.findByUserId(kiosqueroUserId(cognitoSubject), page, size);
    }

    public List<ChatMessage> findStudentMessages(UUID studentIdOrUserId, UUID sessionId,
                                                 int page, int size) {
        requireStudentSession(studentIdOrUserId, sessionId);
        return chatMessageRepository.findBySessionId(sessionId, page, size);
    }

    public List<ChatMessage> findStudentMessages(String cognitoSubject, UUID sessionId,
                                                 int page, int size) {
        requireResolvedStudentSession(authenticatedStudentId(cognitoSubject), sessionId);
        return chatMessageRepository.findBySessionId(sessionId, page, size);
    }

    public List<ChatMessage> findTutorMessages(String cognitoSubject, UUID sessionId,
                                               int page, int size) {
        requireUserSession(tutorUserId(cognitoSubject), sessionId);
        return chatMessageRepository.findBySessionId(sessionId, page, size);
    }

    public List<ChatMessage> findKiosqueroMessages(String cognitoSubject, UUID sessionId,
                                                   int page, int size) {
        requireUserSession(kiosqueroUserId(cognitoSubject), sessionId);
        return chatMessageRepository.findBySessionId(sessionId, page, size);
    }

    public void closeStudentSession(UUID studentIdOrUserId, UUID sessionId) {
        requireStudentSession(studentIdOrUserId, sessionId);
        chatSessionRepository.close(sessionId, LocalDateTime.now(clock));
    }

    public void closeStudentSession(String cognitoSubject, UUID sessionId) {
        requireResolvedStudentSession(authenticatedStudentId(cognitoSubject), sessionId);
        chatSessionRepository.close(sessionId, LocalDateTime.now(clock));
    }

    public void closeTutorSession(String cognitoSubject, UUID sessionId) {
        requireUserSession(tutorUserId(cognitoSubject), sessionId);
        chatSessionRepository.close(sessionId, LocalDateTime.now(clock));
    }

    public void closeKiosqueroSession(String cognitoSubject, UUID sessionId) {
        requireUserSession(kiosqueroUserId(cognitoSubject), sessionId);
        chatSessionRepository.close(sessionId, LocalDateTime.now(clock));
    }

    @Transactional
    public void deleteStudentSession(UUID studentIdOrUserId, UUID sessionId) {
        requireStudentSession(studentIdOrUserId, sessionId);
        deleteSession(sessionId);
    }

    @Transactional
    public void deleteStudentSession(String cognitoSubject, UUID sessionId) {
        requireResolvedStudentSession(authenticatedStudentId(cognitoSubject), sessionId);
        deleteSession(sessionId);
    }

    @Transactional
    public void deleteTutorSession(String cognitoSubject, UUID sessionId) {
        requireUserSession(tutorUserId(cognitoSubject), sessionId);
        deleteSession(sessionId);
    }

    @Transactional
    public void deleteKiosqueroSession(String cognitoSubject, UUID sessionId) {
        requireUserSession(kiosqueroUserId(cognitoSubject), sessionId);
        deleteSession(sessionId);
    }

    private void deleteSession(UUID sessionId) {
        chatMessageRepository.deleteBySessionId(sessionId);
        chatSessionRepository.deleteById(sessionId);
    }

    private void requireStudentSession(UUID studentIdOrUserId, UUID sessionId) {
        UUID studentId = resolvedStudentId(studentIdOrUserId);
        requireResolvedStudentSession(studentId, sessionId);
    }

    private void requireResolvedStudentSession(UUID studentId, UUID sessionId) {
        chatSessionRepository.findByIdAndStudentId(sessionId, studentId)
                .orElseThrow(() -> new ChatSessionNotFoundException(sessionId));
    }

    private UUID resolvedStudentId(UUID studentIdOrUserId) {
        return alumnoRepository.existsById(studentIdOrUserId)
                ? studentIdOrUserId
                : alumnoRepository.findStudentIdByUserId(studentIdOrUserId)
                        .orElseThrow(() -> new AlumnoNoEncontradoException(studentIdOrUserId));
    }

    private void requireUserSession(UUID userId, UUID sessionId) {
        chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ChatSessionNotFoundException(sessionId));
    }

    private UUID tutorUserId(String cognitoSubject) {
        Usuario user = authenticatedUser(cognitoSubject);
        tutorRepository.findByUsuarioIdCognito(cognitoSubject)
                .orElseThrow(() -> new TutorNoEncontradoException(cognitoSubject));
        return user.getId();
    }

    private UUID kiosqueroUserId(String cognitoSubject) {
        Usuario user = authenticatedUser(cognitoSubject);
        UUID buffetId = kiosqueroRepository.findBuffetByUsuarioId(user.getId());

        if (buffetId == null) {
            KiosqueroNotFoundException exception =
                    new KiosqueroNotFoundException(cognitoSubject);
            throw exception;
        }

        return user.getId();
    }

    private UUID authenticatedStudentId(String cognitoSubject) {
        Usuario user = authenticatedUser(cognitoSubject);
        return resolvedStudentId(user.getId());
    }

    private Usuario authenticatedUser(String cognitoSubject) {
        return usuarioRepository.findByIdCognito(cognitoSubject)
                .orElseThrow(() -> new UsuarioAutenticadoNoEncontradoException(cognitoSubject));
    }
}
