package com.recred.backend.domain.services;

import com.recred.backend.domain.models.SchoolAssistantDraft;
import com.recred.backend.domain.models.SchoolAssistantPrompt;
import java.util.Optional;

public interface SchoolAssistantResponder {
    Optional<SchoolAssistantDraft> respond(SchoolAssistantPrompt prompt);
}
