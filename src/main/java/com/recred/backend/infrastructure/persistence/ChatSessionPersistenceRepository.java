package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.exceptions.StudentNotFoundException;
import com.recred.backend.domain.models.ChatSession;
import com.recred.backend.domain.models.ChatSessionState;
import com.recred.backend.domain.repositories.ChatSessionRepository;
import com.recred.backend.infrastructure.entities.ChatSessionEntity;
import com.recred.backend.infrastructure.entities.UserEntity;
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
                        ChatSessionState.ABIERTA
                )
                .map(this::toDomain);
    }

    @Override
    public ChatSession startForStudent(UUID studentId, LocalDateTime startedAt) {
        UUID userId = alumnoRepository.findUserIdByStudentId(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));

        ChatSessionEntity entity = new ChatSessionEntity();
        entity.setUserEntity(entityManager.getReference(UserEntity.class, userId));
        entity.setState(ChatSessionState.ABIERTA);
        entity.setStartDate(startedAt);
        entity.setLastActivityDate(startedAt);

        return toDomain(chatSessionRepository.save(entity));
    }

    @Override
    public void updateLastActivity(UUID sessionId, LocalDateTime lastActivityAt) {
        chatSessionRepository.findById(sessionId)
                .ifPresent(entity -> updateLastActivity(entity, lastActivityAt));
    }

    private void updateLastActivity(ChatSessionEntity entity, LocalDateTime lastActivityAt) {
        entity.setLastActivityDate(lastActivityAt);
        chatSessionRepository.save(entity);
    }

    private ChatSession toDomain(ChatSessionEntity entity) {
        return new ChatSession(
                entity.getId(),
                entity.getState(),
                entity.getStartDate(),
                entity.getLastActivityDate()
        );
    }
}
