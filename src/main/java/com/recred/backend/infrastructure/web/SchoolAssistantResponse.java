package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.recred.backend.domain.models.SchoolAssistantAnswer;
import com.recred.backend.domain.models.SchoolAssistantCapability;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SchoolAssistantResponse(
        @JsonProperty("sesionId")
        UUID sessionId,
        @JsonProperty("respuesta")
        String answer,
        @JsonProperty("capacidades")
        List<SchoolAssistantCapability> capabilities,
        @JsonProperty("fechaHora")
        LocalDateTime dateTime,
        @JsonProperty("generadoPorIa")
        boolean generatedByAi,
        @JsonProperty("modelo")
        String model
) {

    public static SchoolAssistantResponse fromDomain(SchoolAssistantAnswer answer) {
        return new SchoolAssistantResponse(
                answer.sessionId(),
                answer.answer(),
                answer.capabilities(),
                answer.dateTime(),
                answer.generatedByAi(),
                answer.model()
        );
    }
}
