package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.ChatSession;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository {
    Optional<ChatSession> findOpenByIdAndStudentId(UUID sessionId, UUID studentId);

    ChatSession startForStudent(UUID studentId, LocalDateTime startedAt);

    void updateLastActivity(UUID sessionId, LocalDateTime lastActivityAt);
}
