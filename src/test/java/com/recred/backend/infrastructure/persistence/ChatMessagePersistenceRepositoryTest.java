package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.ChatMessageRole;
import com.recred.backend.infrastructure.entities.ChatMessageEntity;
import com.recred.backend.infrastructure.entities.ChatSessionEntity;
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
        ChatSessionEntity session = new ChatSessionEntity();
        ChatMessagePersistenceRepository repository = new ChatMessagePersistenceRepository(
                chatMessageRepository,
                entityManager
        );

        when(entityManager.getReference(ChatSessionEntity.class, sessionId)).thenReturn(session);

        repository.save(new ChatMessage(
                sessionId,
                ChatMessageRole.USUARIO,
                "saldo",
                Map.of("capacidades", List.of("SALDO")),
                now
        ));

        ArgumentCaptor<ChatMessageEntity> captor = ArgumentCaptor
                .forClass(ChatMessageEntity.class);
        verify(chatMessageRepository).save(captor.capture());
        ChatMessageEntity saved = captor.getValue();
        assertSame(session, saved.getSession());
        assertEquals(ChatMessageRole.USUARIO, saved.getRole());
        assertEquals("saldo", saved.getContent());
        assertEquals(now, saved.getDateAndTime());
    }

    @Test
    void findRecentBySessionIdMapsMessagesToDomain() {
        UUID sessionId = UUID.randomUUID();
        ChatSessionEntity session = new ChatSessionEntity();
        session.setId(sessionId);
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setSession(session);
        entity.setRole(ChatMessageRole.ASISTENTE_IA);
        entity.setContent("Respuesta");
        entity.setMetadata(Map.of("generadoPorIa", true));
        entity.setDateAndTime(LocalDateTime.of(2026, 5, 27, 12, 0));
        ChatMessagePersistenceRepository repository = new ChatMessagePersistenceRepository(
                chatMessageRepository,
                entityManager
        );

        when(chatMessageRepository.findRecentBySessionId(sessionId, PageRequest.of(0, 2)))
                .thenReturn(List.of(entity));

        List<ChatMessage> messages = repository.findRecentBySessionId(sessionId, 2);

        assertEquals(1, messages.size());
        assertEquals(sessionId, messages.get(0).sessionId());
        assertEquals(ChatMessageRole.ASISTENTE_IA, messages.get(0).role());
        assertEquals("Respuesta", messages.get(0).content());
    }
}
