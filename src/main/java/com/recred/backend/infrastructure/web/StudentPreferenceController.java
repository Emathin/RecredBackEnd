package com.recred.backend.infrastructure.web;

import com.recred.backend.application.DetectStudentPreferences;
import com.recred.backend.application.GetStudentPreferences;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

    public StudentPreferenceController(
            DetectStudentPreferences detectStudentPreferences,
            GetStudentPreferences getStudentPreferences) {
        this.detectStudentPreferences = detectStudentPreferences;
        this.getStudentPreferences = getStudentPreferences;
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
            @RequestParam(name = "ultima", required = false, defaultValue = "false") boolean ultima
    ) {
        List<StudentPreferenceResponse> preferences = getStudentPreferences
                .execute(alumnoId, ultima)
                .stream()
                .map(StudentPreferenceResponse::fromEntity)
                .toList();

        return preferences.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(preferences);
    }
}
