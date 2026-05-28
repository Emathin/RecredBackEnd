package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.models.ChatSession;
import com.recred.backend.domain.models.EstadoChatSesion;
import com.recred.backend.domain.repositories.ChatSessionRepository;
import com.recred.backend.infrastructure.entities.ChatSesionEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
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
    public ChatSession startForStudent(UUID studentId, LocalDateTime startedAt) {
        UUID userId = alumnoRepository.findUserIdByStudentId(studentId)
                .orElseThrow(() -> new AlumnoNoEncontradoException(studentId));

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

    private void updateLastActivity(ChatSesionEntity entity, LocalDateTime lastActivityAt) {
        entity.setFechaUltimaActividad(lastActivityAt);
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
