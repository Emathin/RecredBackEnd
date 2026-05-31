package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ChatSession;
import com.recred.backend.domain.models.ChatSessionState;
import com.recred.backend.infrastructure.entities.ChatSessionEntity;
import com.recred.backend.infrastructure.entities.UserEntity;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatSessionPersistenceRepositoryTest {

    @Mock
    private SpringDataChatSessionRepository chatSessionRepository;

    @Mock
    private SpringDataAlumnoRepository alumnoRepository;

    @Mock
    private EntityManager entityManager;

    @Test
    void startForStudentCreatesOpenSessionForStudentUser() {
        UUID studentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2026, 5, 27, 12, 0);
        UserEntity user = new UserEntity();
        ChatSessionPersistenceRepository repository = repository();

        when(alumnoRepository.findUserIdByStudentId(studentId)).thenReturn(Optional.of(userId));
        when(entityManager.getReference(UserEntity.class, userId)).thenReturn(user);
        when(chatSessionRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    ChatSessionEntity entity = invocation.getArgument(0);
                    entity.setId(sessionId);
                    return entity;
                });

        ChatSession session = repository.startForStudent(studentId, now);

        assertEquals(sessionId, session.id());
        assertEquals(ChatSessionState.ABIERTA, session.status());
        ArgumentCaptor<ChatSessionEntity> captor = ArgumentCaptor
                .forClass(ChatSessionEntity.class);
        verify(chatSessionRepository).save(captor.capture());
        ChatSessionEntity saved = captor.getValue();
        assertSame(user, saved.getUserEntity());
        assertEquals(now, saved.getStartDate());
        assertEquals(now, saved.getLastActivityDate());
    }

    @Test
    void findOpenByIdAndStudentIdMapsEntityToDomain() {
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2026, 5, 27, 12, 0);
        ChatSessionEntity entity = new ChatSessionEntity();
        entity.setId(sessionId);
        entity.setState(ChatSessionState.ABIERTA);
        entity.setStartDate(now.minusMinutes(5));
        entity.setLastActivityDate(now);
        ChatSessionPersistenceRepository repository = repository();

        when(chatSessionRepository.findOpenByIdAndStudentId(
                sessionId,
                studentId,
                ChatSessionState.ABIERTA
        )).thenReturn(Optional.of(entity));

        Optional<ChatSession> session = repository.findOpenByIdAndStudentId(
                sessionId,
                studentId
        );

        assertEquals(sessionId, session.orElseThrow().id());
        assertEquals(now, session.orElseThrow().lastActivityAt());
    }

    private ChatSessionPersistenceRepository repository() {
        return new ChatSessionPersistenceRepository(
                chatSessionRepository,
                alumnoRepository,
                entityManager
        );
    }
}
