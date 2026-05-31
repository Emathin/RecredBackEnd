package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.repositories.ChatMessageRepository;
import com.recred.backend.infrastructure.entities.ChatMessageEntity;
import com.recred.backend.infrastructure.entities.ChatSessionEntity;
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
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setSession(entityManager.getReference(ChatSessionEntity.class, message.sessionId()));
        entity.setRole(message.role());
        entity.setContent(message.content());
        entity.setMetadata(message.metadata());
        entity.setDateAndTime(message.dateTime());

        chatMessageRepository.save(entity);
    }

    @Override
    public List<ChatMessage> findRecentBySessionId(UUID sessionId, int limit) {
        return chatMessageRepository.findRecentBySessionId(sessionId, PageRequest.of(0, limit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private ChatMessage toDomain(ChatMessageEntity entity) {
        return new ChatMessage(
                entity.getSession().getId(),
                entity.getRole(),
                entity.getContent(),
                entity.getMetadata(),
                entity.getDateAndTime()
        );
    }
}
