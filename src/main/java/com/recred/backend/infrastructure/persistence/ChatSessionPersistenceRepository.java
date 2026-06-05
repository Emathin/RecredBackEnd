package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.models.ChatSession;
import com.recred.backend.domain.models.EstadoChatSesion;
import com.recred.backend.domain.repositories.ChatSessionRepository;
import com.recred.backend.infrastructure.entities.ChatSesionEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class ChatSessionPersistenceRepository implements ChatSessionRepository {

    private final SpringDataChatSessionRepository chatSessionRepository;
    private final SpringDataAlumnoRepository alumnoRepository;
    private final EntityManager entityManager;

    public ChatSessionPersistenceRepository(
            SpringDataChatSessionRepository chatSessionRepository,
            SpringDataAlumnoRepository alumnoRepository,
            EntityManager entityManager
    ) {
        this.chatSessionRepository = chatSessionRepository;
        this.alumnoRepository = alumnoRepository;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<ChatSession> findOpenByIdAndStudentId(UUID sessionId, UUID studentId) {
        return chatSessionRepository.findOpenByIdAndStudentId(
                        sessionId,
                        studentId,
                        EstadoChatSesion.ABIERTA
                )
                .map(this::toDomain);
    }

    @Override
    public Optional<ChatSession> findOpenByIdAndUserId(UUID sessionId, UUID userId) {
        return chatSessionRepository.findOpenByIdAndUserId(
                        sessionId,
                        userId,
                        EstadoChatSesion.ABIERTA
                )
                .map(this::toDomain);
    }

    @Override
    public Optional<ChatSession> findByIdAndStudentId(UUID sessionId, UUID studentId) {
        return chatSessionRepository.findByIdAndStudentId(sessionId, studentId)
                .map(this::toDomain);
    }

    @Override
    public Optional<ChatSession> findByIdAndUserId(UUID sessionId, UUID userId) {
        return chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .map(this::toDomain);
    }

    @Override
    public List<ChatSession> findByStudentId(UUID studentId, int page, int size) {
        return chatSessionRepository.findByStudentId(studentId, PageRequest.of(page, size))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<ChatSession> findByUserId(UUID userId, int page, int size) {
        return chatSessionRepository.findByUserId(userId, PageRequest.of(page, size))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public ChatSession startForStudent(UUID studentId, LocalDateTime startedAt) {
        UUID userId = alumnoRepository.findUserIdByStudentId(studentId)
                .orElseThrow(() -> new AlumnoNoEncontradoException(studentId));

        return startForUser(userId, startedAt);
    }

    @Override
    public ChatSession startForUser(UUID userId, LocalDateTime startedAt) {
        ChatSesionEntity entity = new ChatSesionEntity();
        entity.setUsuarioEntity(entityManager.getReference(UsuarioEntity.class, userId));
        entity.setEstado(EstadoChatSesion.ABIERTA);
        entity.setFechaInicio(startedAt);
        entity.setFechaUltimaActividad(startedAt);

        return toDomain(chatSessionRepository.save(entity));
    }

    @Override
    public void updateLastActivity(UUID sessionId, LocalDateTime lastActivityAt) {
        chatSessionRepository.findById(sessionId)
                .ifPresent(entity -> updateLastActivity(entity, lastActivityAt));
    }

    @Override
    public void close(UUID sessionId, LocalDateTime closedAt) {
        chatSessionRepository.findById(sessionId)
                .ifPresent(entity -> close(entity, closedAt));
    }

    @Override
    public void deleteById(UUID sessionId) {
        chatSessionRepository.deleteById(sessionId);
    }

    private void updateLastActivity(ChatSesionEntity entity, LocalDateTime lastActivityAt) {
        entity.setFechaUltimaActividad(lastActivityAt);
        chatSessionRepository.save(entity);
    }

    private void close(ChatSesionEntity entity, LocalDateTime closedAt) {
        entity.setEstado(EstadoChatSesion.CERRADA);
        entity.setFechaUltimaActividad(closedAt);
        chatSessionRepository.save(entity);
    }

    private ChatSession toDomain(ChatSesionEntity entity) {
        return new ChatSession(
                entity.getId(),
                entity.getEstado(),
                entity.getFechaInicio(),
                entity.getFechaUltimaActividad()
        );
    }
}
