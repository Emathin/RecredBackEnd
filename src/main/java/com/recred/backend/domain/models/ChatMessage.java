package com.recred.backend.domain.models;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record ChatMessage(
        UUID sessionId,
        ChatMessageRole role,
        String content,
        Map<String, Object> metadata,
        LocalDateTime dateTime
) {
}
