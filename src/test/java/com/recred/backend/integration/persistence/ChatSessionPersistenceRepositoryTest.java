package com.recred.backend.integration.persistence;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.models.ChatSession;
import com.recred.backend.domain.models.EstadoChatSesion;
import com.recred.backend.infrastructure.entities.ChatSesionEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.ChatSessionPersistenceRepository;
import com.recred.backend.infrastructure.persistence.SpringDataAlumnoRepository;
import com.recred.backend.infrastructure.persistence.SpringDataChatSessionRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        UsuarioEntity user = new UsuarioEntity();
        ChatSessionPersistenceRepository repository = repository();

        when(alumnoRepository.findUserIdByStudentId(studentId)).thenReturn(Optional.of(userId));
        when(entityManager.getReference(UsuarioEntity.class, userId)).thenReturn(user);
        when(chatSessionRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    ChatSesionEntity entity = invocation.getArgument(0);
                    entity.setId(sessionId);
                    return entity;
                });

        ChatSession session = repository.startForStudent(studentId, now);

        assertEquals(sessionId, session.id());
        assertEquals(EstadoChatSesion.ABIERTA, session.status());
        ArgumentCaptor<ChatSesionEntity> captor = ArgumentCaptor
                .forClass(ChatSesionEntity.class);
        verify(chatSessionRepository).save(captor.capture());
        ChatSesionEntity saved = captor.getValue();
        assertSame(user, saved.getUsuarioEntity());
        assertEquals(now, saved.getFechaInicio());
        assertEquals(now, saved.getFechaUltimaActividad());
    }

    @Test
    void findOpenByIdAndStudentIdMapsEntityToDomain() {
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2026, 5, 27, 12, 0);
        ChatSesionEntity entity = entity(sessionId, EstadoChatSesion.ABIERTA, now);
        ChatSessionPersistenceRepository repository = repository();

        when(chatSessionRepository.findOpenByIdAndStudentId(
                sessionId,
                studentId,
                EstadoChatSesion.ABIERTA
        )).thenReturn(Optional.of(entity));

        Optional<ChatSession> session = repository.findOpenByIdAndStudentId(
                sessionId,
                studentId
        );

        assertEquals(sessionId, session.orElseThrow().id());
        assertEquals(now, session.orElseThrow().lastActivityAt());
    }

    @Test
    void findOpenByIdAndUserIdMapsEntityToDomain() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2026, 5, 27, 12, 0);
        ChatSesionEntity entity = entity(sessionId, EstadoChatSesion.ABIERTA, now);
        ChatSessionPersistenceRepository repository = repository();

        when(chatSessionRepository.findOpenByIdAndUserId(
                sessionId,
                userId,
                EstadoChatSesion.ABIERTA
        )).thenReturn(Optional.of(entity));

        Optional<ChatSession> session = repository.findOpenByIdAndUserId(sessionId, userId);

        assertTrue(session.isPresent());
        assertEquals(sessionId, session.orElseThrow().id());
    }

    @Test
    void findByIdMethodsMapEntitiesToDomain() {
        UUID studentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        ChatSesionEntity entity = entity(
                sessionId,
                EstadoChatSesion.CERRADA,
                LocalDateTime.now()
        );
        ChatSessionPersistenceRepository repository = repository();
        when(chatSessionRepository.findByIdAndStudentId(sessionId, studentId))
                .thenReturn(Optional.of(entity));
        when(chatSessionRepository.findByIdAndUserId(sessionId, userId))
                .thenReturn(Optional.of(entity));

        assertEquals(EstadoChatSesion.CERRADA,
                repository.findByIdAndStudentId(sessionId, studentId).orElseThrow().status());
        assertEquals(EstadoChatSesion.CERRADA,
                repository.findByIdAndUserId(sessionId, userId).orElseThrow().status());
    }

    @Test
    void findByStudentIdAndUserIdShouldUsePageRequest() {
        UUID studentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ChatSesionEntity entity = entity(
                UUID.randomUUID(),
                EstadoChatSesion.ABIERTA,
                LocalDateTime.now()
        );
        ChatSessionPersistenceRepository repository = repository();
        when(chatSessionRepository.findByStudentId(studentId, PageRequest.of(1, 5)))
                .thenReturn(List.of(entity));
        when(chatSessionRepository.findByUserId(userId, PageRequest.of(0, 10)))
                .thenReturn(List.of(entity));

        assertEquals(1, repository.findByStudentId(studentId, 1, 5).size());
        assertEquals(1, repository.findByUserId(userId, 0, 10).size());
    }

    @Test
    void startForStudent_WhenStudentDoesNotExist_ShouldFail() {
        UUID studentId = UUID.randomUUID();
        ChatSessionPersistenceRepository repository = repository();
        when(alumnoRepository.findUserIdByStudentId(studentId)).thenReturn(Optional.empty());

        assertThrows(AlumnoNoEncontradoException.class,
                () -> repository.startForStudent(studentId, LocalDateTime.now()));
    }

    @Test
    void updateLastActivity_WhenSessionExists_ShouldSaveUpdatedEntity() {
        UUID sessionId = UUID.randomUUID();
        LocalDateTime lastActivity = LocalDateTime.of(2026, 6, 15, 13, 0);
        ChatSesionEntity entity = entity(
                sessionId,
                EstadoChatSesion.ABIERTA,
                lastActivity.minusHours(1)
        );
        ChatSessionPersistenceRepository repository = repository();
        when(chatSessionRepository.findById(sessionId)).thenReturn(Optional.of(entity));

        repository.updateLastActivity(sessionId, lastActivity);

        assertEquals(lastActivity, entity.getFechaUltimaActividad());
        verify(chatSessionRepository).save(entity);
    }

    @Test
    void close_WhenSessionExists_ShouldCloseAndSaveEntity() {
        UUID sessionId = UUID.randomUUID();
        LocalDateTime closedAt = LocalDateTime.of(2026, 6, 15, 13, 0);
        ChatSesionEntity entity = entity(
                sessionId,
                EstadoChatSesion.ABIERTA,
                closedAt.minusHours(1)
        );
        ChatSessionPersistenceRepository repository = repository();
        when(chatSessionRepository.findById(sessionId)).thenReturn(Optional.of(entity));

        repository.close(sessionId, closedAt);

        assertEquals(EstadoChatSesion.CERRADA, entity.getEstado());
        assertEquals(closedAt, entity.getFechaUltimaActividad());
        verify(chatSessionRepository).save(entity);
    }

    @Test
    void deleteById_ShouldDelegate() {
        UUID sessionId = UUID.randomUUID();

        repository().deleteById(sessionId);

        verify(chatSessionRepository).deleteById(sessionId);
    }

    private ChatSessionPersistenceRepository repository() {
        return new ChatSessionPersistenceRepository(
                chatSessionRepository,
                alumnoRepository,
                entityManager
        );
    }

    private ChatSesionEntity entity(
            UUID sessionId,
            EstadoChatSesion status,
            LocalDateTime lastActivity
    ) {
        ChatSesionEntity entity = new ChatSesionEntity();
        entity.setId(sessionId);
        entity.setEstado(status);
        entity.setFechaInicio(lastActivity.minusMinutes(5));
        entity.setFechaUltimaActividad(lastActivity);
        return entity;
    }
}
