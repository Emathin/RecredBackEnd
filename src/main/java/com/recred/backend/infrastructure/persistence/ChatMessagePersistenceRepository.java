package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.repositories.ChatMessageRepository;
import com.recred.backend.infrastructure.entities.ChatMensajeEntity;
import com.recred.backend.infrastructure.entities.ChatSesionEntity;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class ChatMessagePersistenceRepository implements ChatMessageRepository {

    private final SpringDataChatMessageRepository chatMessageRepository;
    private final EntityManager entityManager;

    public ChatMessagePersistenceRepository(
            SpringDataChatMessageRepository chatMessageRepository,
            EntityManager entityManager
    ) {
        this.chatMessageRepository = chatMessageRepository;
        this.entityManager = entityManager;
    }

    @Override
    public void save(ChatMessage message) {
        ChatMensajeEntity entity = new ChatMensajeEntity();
        entity.setSesion(entityManager.getReference(ChatSesionEntity.class, message.sessionId()));
        entity.setRol(message.role());
        entity.setContenido(message.content());
        entity.setMetadata(message.metadata());
        entity.setFechaHora(message.dateTime());

        chatMessageRepository.save(entity);
    }

    @Override
    public List<ChatMessage> findRecentBySessionId(UUID sessionId, int limit) {
        return chatMessageRepository.findRecentBySessionId(sessionId, PageRequest.of(0, limit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<ChatMessage> findBySessionId(UUID sessionId, int page, int size) {
        return chatMessageRepository.findBySessionId(sessionId, PageRequest.of(page, size))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteBySessionId(UUID sessionId) {
        chatMessageRepository.deleteBySessionId(sessionId);
    }

    private ChatMessage toDomain(ChatMensajeEntity entity) {
        return new ChatMessage(
                entity.getId(),
                entity.getSesion().getId(),
                entity.getRol(),
                entity.getContenido(),
                entity.getMetadata(),
                entity.getFechaHora()
        );
    }
}
