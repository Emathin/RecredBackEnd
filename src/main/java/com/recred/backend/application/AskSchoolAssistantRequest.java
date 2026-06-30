package com.recred.backend.application;

import com.recred.backend.domain.models.SchoolAssistantRole;
import java.util.Optional;
import java.util.UUID;

public record AskSchoolAssistantRequest(
        String cognitoSubject,
        SchoolAssistantRole role,
        UUID sessionId,
        String message
) {

    public Optional<UUID> optionalSessionId() {
        return Optional.ofNullable(sessionId);
    }
}
