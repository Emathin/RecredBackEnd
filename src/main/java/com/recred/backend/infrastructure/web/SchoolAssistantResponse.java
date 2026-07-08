package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.recred.backend.application.AskSchoolAssistantResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SchoolAssistantResponse(
        @JsonProperty("sesionId")
        UUID sessionId,
        @JsonProperty("respuesta")
        String answer,
        @JsonProperty("capacidades")
        List<String> capabilities,
        @JsonProperty("fechaHora")
        LocalDateTime dateTime,
        @JsonProperty("generadoPorIa")
        boolean generatedByAi,
        @JsonProperty("modelo")
        String model,
        @JsonProperty("rol")
        String role,
        @JsonProperty("intencion")
        String intent,
        @JsonProperty("accion")
        SchoolAssistantActionResponse action,
        @JsonProperty("sugerencias")
        List<SchoolAssistantSuggestionResponse> suggestions,
        @JsonProperty("datos")
        Map<String, Object> data
) {

    public static SchoolAssistantResponse fromApplication(AskSchoolAssistantResponse answer) {
        return new SchoolAssistantResponse(
                answer.sessionId(),
                answer.answer(),
                answer.capabilities(),
                answer.dateTime(),
                answer.generatedByAi(),
                answer.model(),
                answer.role(),
                answer.intent(),
                SchoolAssistantActionResponse.fromApplication(answer.action()),
                answer.suggestions().stream()
                        .map(SchoolAssistantSuggestionResponse::fromApplication)
                        .toList(),
                answer.data()
        );
    }
}
