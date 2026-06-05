package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.ChatSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository {
    Optional<ChatSession> findOpenByIdAndStudentId(UUID sessionId, UUID studentId);

    Optional<ChatSession> findOpenByIdAndUserId(UUID sessionId, UUID userId);

    Optional<ChatSession> findByIdAndStudentId(UUID sessionId, UUID studentId);

    Optional<ChatSession> findByIdAndUserId(UUID sessionId, UUID userId);

    List<ChatSession> findByStudentId(UUID studentId, int page, int size);

    List<ChatSession> findByUserId(UUID userId, int page, int size);

    ChatSession startForStudent(UUID studentId, LocalDateTime startedAt);

    ChatSession startForUser(UUID userId, LocalDateTime startedAt);

    void updateLastActivity(UUID sessionId, LocalDateTime lastActivityAt);

    void close(UUID sessionId, LocalDateTime closedAt);

    void deleteById(UUID sessionId);
}
