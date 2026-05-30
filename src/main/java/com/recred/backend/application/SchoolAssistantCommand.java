package com.recred.backend.application;

import java.util.Optional;
import java.util.UUID;

public record SchoolAssistantCommand(
        Optional<UUID> sessionId,
        String message
) {
}
