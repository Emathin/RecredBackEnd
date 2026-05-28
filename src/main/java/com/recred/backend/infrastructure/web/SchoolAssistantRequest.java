package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import java.util.UUID;

public record SchoolAssistantRequest(
        @JsonProperty("sesionId")
        UUID sessionId,
        @JsonProperty("mensaje")
        String message
) {

    public Optional<UUID> optionalSessionId() {
        return Optional.ofNullable(sessionId);
    }
}
