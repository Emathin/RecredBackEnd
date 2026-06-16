package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.RolChatMensaje;
import com.recred.backend.infrastructure.entities.ChatMensajeEntity;
import com.recred.backend.infrastructure.entities.ChatSesionEntity;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessagePersistenceRepositoryTest {

    @Mock
    private SpringDataChatMessageRepository chatMessageRepository;

    @Mock
    private EntityManager entityManager;

    @Test
    void saveMapsMessageToEntity() {
        UUID sessionId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2026, 5, 27, 12, 0);
        ChatSesionEntity session = new ChatSesionEntity();
        ChatMessagePersistenceRepository repository = new ChatMessagePersistenceRepository(
                chatMessageRepository,
                entityManager
        );

        when(entityManager.getReference(ChatSesionEntity.class, sessionId)).thenReturn(session);

        repository.save(new ChatMessage(
                sessionId,
                RolChatMensaje.USUARIO,
                "saldo",
                Map.of("capacidades", List.of("SALDO")),
                now
        ));

        ArgumentCaptor<ChatMensajeEntity> captor = ArgumentCaptor
                .forClass(ChatMensajeEntity.class);
        verify(chatMessageRepository).save(captor.capture());
        ChatMensajeEntity saved = captor.getValue();
        assertSame(session, saved.getSesion());
        assertEquals(RolChatMensaje.USUARIO, saved.getRol());
        assertEquals("saldo", saved.getContenido());
        assertEquals(now, saved.getFechaHora());
    }

    @Test
    void findRecentBySessionIdMapsMessagesToDomain() {
        UUID sessionId = UUID.randomUUID();
        ChatSesionEntity session = new ChatSesionEntity();
        session.setId(sessionId);
        ChatMensajeEntity entity = new ChatMensajeEntity();
        entity.setSesion(session);
        entity.setRol(RolChatMensaje.ASISTENTE_IA);
        entity.setContenido("Respuesta");
        entity.setMetadata(Map.of("generadoPorIa", true));
        entity.setFechaHora(LocalDateTime.of(2026, 5, 27, 12, 0));
        ChatMessagePersistenceRepository repository = new ChatMessagePersistenceRepository(
                chatMessageRepository,
                entityManager
        );

        when(chatMessageRepository.findRecentBySessionId(sessionId, PageRequest.of(0, 2)))
                .thenReturn(List.of(entity));

        List<ChatMessage> messages = repository.findRecentBySessionId(sessionId, 2);

        assertEquals(1, messages.size());
        assertEquals(sessionId, messages.get(0).sessionId());
        assertEquals(RolChatMensaje.ASISTENTE_IA, messages.get(0).role());
        assertEquals("Respuesta", messages.get(0).content());
    }

    @Test
    void findBySessionIdMapsPagedMessagesToDomain() {
        UUID sessionId = UUID.randomUUID();
        ChatSesionEntity session = new ChatSesionEntity();
        session.setId(sessionId);
        ChatMensajeEntity entity = new ChatMensajeEntity();
        entity.setId(UUID.randomUUID());
        entity.setSesion(session);
        entity.setRol(RolChatMensaje.USUARIO);
        entity.setContenido("Pregunta");
        entity.setMetadata(Map.of("origen", "test"));
        entity.setFechaHora(LocalDateTime.of(2026, 5, 27, 13, 0));
        ChatMessagePersistenceRepository repository = new ChatMessagePersistenceRepository(
                chatMessageRepository,
                entityManager
        );

        when(chatMessageRepository.findBySessionId(sessionId, PageRequest.of(1, 5)))
                .thenReturn(List.of(entity));

        List<ChatMessage> messages = repository.findBySessionId(sessionId, 1, 5);

        assertEquals(1, messages.size());
        assertEquals(entity.getId(), messages.get(0).id());
        assertEquals(sessionId, messages.get(0).sessionId());
        assertEquals(RolChatMensaje.USUARIO, messages.get(0).role());
    }

    @Test
    void deleteBySessionIdDelegatesToSpringDataRepository() {
        UUID sessionId = UUID.randomUUID();
        ChatMessagePersistenceRepository repository = new ChatMessagePersistenceRepository(
                chatMessageRepository,
                entityManager
        );

        repository.deleteBySessionId(sessionId);

        verify(chatMessageRepository).deleteBySessionId(sessionId);
    }
}
