package com.recred.backend.infrastructure.web;

import com.recred.backend.application.DetectStudentPreferences;
import com.recred.backend.application.GetStudentPreferences;
import com.recred.backend.application.RespondToAISuggestion;
import com.recred.backend.domain.models.IaSuggestionType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alumnos/{alumnoId}/preferencias")
public class StudentPreferenceController {

    private final DetectStudentPreferences detectStudentPreferences;
    private final GetStudentPreferences getStudentPreferences;
    private final RespondToAISuggestion respondToAISuggestion;

    public StudentPreferenceController(
            DetectStudentPreferences detectStudentPreferences,
            GetStudentPreferences getStudentPreferences,
            RespondToAISuggestion respondToAISuggestion) {
        this.detectStudentPreferences = detectStudentPreferences;
        this.getStudentPreferences = getStudentPreferences;
        this.respondToAISuggestion = respondToAISuggestion;
    }

    @PostMapping("/detectar")
    public ResponseEntity<StudentPreferenceResponse> detect(
            @PathVariable UUID alumnoId,
            @RequestParam("buffetId") UUID buffetId
    ) {
        return detectStudentPreferences.execute(alumnoId, buffetId)
                .map(StudentPreferenceResponse::fromDomain)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping
    public ResponseEntity<List<StudentPreferenceResponse>> getPreferences(
            @PathVariable UUID alumnoId,
            @RequestParam(name = "ultima", required = false, defaultValue = "false") boolean ultima,
            @RequestParam(name = "tipo", required = false) IaSuggestionType tipo
    ) {
        List<StudentPreferenceResponse> preferences = getStudentPreferences
                .execute(alumnoId, ultima, tipo)
                .stream()
                .map(StudentPreferenceResponse::fromEntity)
                .toList();

        return preferences.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(preferences);
    }

    @PatchMapping("/{sugerenciaId}/responder")
    public ResponseEntity<Void> respond(
            @PathVariable UUID sugerenciaId,
            @RequestParam("aceptada") boolean aceptada
    ) {
        respondToAISuggestion.execute(sugerenciaId, aceptada);
        return ResponseEntity.ok().build();
    }
}
