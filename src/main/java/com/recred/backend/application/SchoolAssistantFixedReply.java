package com.recred.backend.application;

import java.util.Map;

public record SchoolAssistantFixedReply(
        String answer,
        Map<String, Object> metadata
) {
}
