package com.recred.backend.application;

import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import java.util.List;
import java.util.Optional;

record SchoolAssistantFixedTopicRequest(
        SchoolAssistantIntent intent,
        SchoolAssistantContext context,
        List<ChatMessage> recentMessages,
        Optional<RecessTime> selectedRecessTime
) {
}
