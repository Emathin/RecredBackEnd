package com.recred.backend.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatSession(
        UUID id,
        ChatSessionState status,
        LocalDateTime startedAt,
        LocalDateTime lastActivityAt
) {
}
