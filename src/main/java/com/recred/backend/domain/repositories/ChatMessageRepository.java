package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.ChatMessage;
import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository {
    void save(ChatMessage message);

    List<ChatMessage> findRecentBySessionId(UUID sessionId, int limit);

    List<ChatMessage> findBySessionId(UUID sessionId, int page, int size);

    void deleteBySessionId(UUID sessionId);
}
